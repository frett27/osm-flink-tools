package org.frett27.spatialflink.inputs.process;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatJoinFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.GroupReduceFunction;
import org.apache.flink.api.common.functions.JoinFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.operators.FlatMapOperator;
import org.apache.flink.api.java.operators.MapOperator;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.typeutils.GenericTypeInfo;
import org.apache.flink.util.Collector;
import org.frett27.spatialflink.inputs.OSMPBFAllEntities;
import org.frett27.spatialflink.model.AttributedEntity;
import org.frett27.spatialflink.model.ComplexEntity;
import org.frett27.spatialflink.model.NodeEntity;
import org.frett27.spatialflink.model.RelatedObject;
import org.frett27.spatialflink.model.Relation;
import org.frett27.spatialflink.model.WayEntity;
import org.frett27.spatialflink.tools.GeometryTools;
import org.frett27.spatialflink.tools.PolygonCreator;
import org.frett27.spatialflink.tools.PolygonCreator.Role;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Geometry.Type;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;

public class ProcessOSM {

	static String convertToString(Map<String, Object> attributes) {
		if (attributes == null)
			return "";

		StringBuilder sb = new StringBuilder();
		for (Entry<String, Object> e : attributes.entrySet()) {
			if (sb.length() > 0)
				sb.append('|');
			sb.append(e.getKey()).append("=").append(e.getValue() == null ? "" : e.getValue());
		}
		return sb.toString();
	}

	public static class OSMResultsStreams {

		DataSet<ComplexEntity> retWaysEntities;
		DataSet<ComplexEntity> retPolygons;
		DataSet<NodeEntity> retNodesWithAttributes;
		DataSet<Relation> retRelations;

	}

	public static OSMResultsStreams constructOSMStreams(ExecutionEnvironment env, String inputFile) throws Exception {

		OSMPBFAllEntities iallformat = new OSMPBFAllEntities();
		iallformat.setFilePath(inputFile);

		DataSource<AttributedEntity> allEntitiesAttributed = env.createInput(iallformat,
				new GenericTypeInfo<AttributedEntity>(AttributedEntity.class));

		// nodes
		DataSet<NodeEntity> nodes = allEntitiesAttributed.flatMap(new FlatMapFunction<AttributedEntity, NodeEntity>() {
			@Override
			public void flatMap(AttributedEntity value, Collector<NodeEntity> out) throws Exception {
				if (value instanceof NodeEntity)
					out.collect((NodeEntity) value);
			}
		});

		// ways
		DataSet<WayEntity> ways = allEntitiesAttributed.flatMap(new FlatMapFunction<AttributedEntity, WayEntity>() {
			@Override
			public void flatMap(AttributedEntity value, Collector<WayEntity> out) throws Exception {
				if (value instanceof WayEntity)
					out.collect((WayEntity) value);
			}
		});

		DataSet<Relation> rels = allEntitiesAttributed.flatMap(new FlatMapFunction<AttributedEntity, Relation>() {
			@Override
			public void flatMap(AttributedEntity value, Collector<Relation> out) throws Exception {
				if (value instanceof Relation)
					out.collect((Relation) value);
			}
		});

		// get only the positions of the nodes
		MapOperator<NodeEntity, Tuple3<Long, Double, Double>> onlypos = nodes
				.map(new MapFunction<NodeEntity, Tuple3<Long, Double, Double>>() {
					@Override
					public Tuple3<Long, Double, Double> map(NodeEntity value) throws Exception {
						return new Tuple3<Long, Double, Double>(value.id, value.x, value.y);
					}
				});

		DataSet<NodeEntity> retNodesWithAttributes = nodes.filter(new FilterFunction<NodeEntity>() {

			@Override
			public boolean filter(NodeEntity value) throws Exception {
				if (value == null)
					return false;
				return value.fields != null && value.fields.size() > 0;
			}
		});

		FlatMapOperator<WayEntity, Tuple3<Long, Long, Integer>> relsLink = ways
				.flatMap(new FlatMapFunction<WayEntity, Tuple3<Long, Long, Integer>>() {
					@Override
					public void flatMap(WayEntity value, Collector<Tuple3<Long, Long, Integer>> out) throws Exception {
						if (value.relatedObjects != null) {
							int c = 0;
							for (RelatedObject r : value.relatedObjects) {
								out.collect(new Tuple3<>(value.id, r.relatedId, c++));
							}
						}

					}

				});

		// relslink contains id, related and order
		// only pos contains id, x, y
		DataSet<Tuple4<Long, Integer, Double, Double>> joinedWaysWithPoints = relsLink.joinWithHuge(onlypos).where(1).equalTo(0)
				.projectFirst(0, 2).projectSecond(1, 2);

		// join on related, keep id, order, x, y

		// group by id and sort on field order
		DataSet<Tuple2<Long, byte[]>> waysGeometry = joinedWaysWithPoints.groupBy(0).sortGroup(1, Order.ASCENDING)
				.reduceGroup(new GroupReduceFunction<Tuple4<Long, Integer, Double, Double>, Tuple2<Long, byte[]>>() {
					@Override
					public void reduce(Iterable<Tuple4<Long, Integer, Double, Double>> values,
							Collector<Tuple2<Long, byte[]>> out) throws Exception {
						long id = -1;

						MultiPath multiPath;

						multiPath = new Polyline();

						boolean started = false;

						for (Tuple4<Long, Integer, Double, Double> t : values) {
							id = t.getField(0);
							double x = t.getField(2);
							double y = t.getField(3);

							if (!started) {
								multiPath.startPath(new Point(x, y));
								started = true;
							} else {
								multiPath.lineTo(new Point(x, y));
							}
						}

						byte[] elements = GeometryEngine.geometryToEsriShape(multiPath);

						out.collect(new Tuple2<Long, byte[]>(id, elements));

					}
				});

		// create the polyline entities

		DataSet<ComplexEntity> retWaysEntities = ways.join(waysGeometry).where(new KeySelector<WayEntity, Long>() {
			@Override
			public Long getKey(WayEntity value) throws Exception {
				return value.id;
			}
		}).equalTo(0).with(new FlatJoinFunction<WayEntity, Tuple2<Long, byte[]>, ComplexEntity>() {
			@Override
			public void join(WayEntity first, Tuple2<Long, byte[]> second, Collector<ComplexEntity> out)
					throws Exception {

				if (first == null) {
					return;
				}

				// take only the ways with attributes ??
				if (first.fields == null || first.fields.size() == 0) {
					return;
				}

				ComplexEntity ce = new ComplexEntity();
				ce.fields = first.fields;
				ce.id = first.id;
				ce.shapeGeometry = second.f1;
				ce.geomType = Type.Polyline;

				out.collect(ce);
			}

		});

		// ways that contains polygons
		DataSet<ComplexEntity> waysAndPolys = retWaysEntities.map(new MapFunction<ComplexEntity, ComplexEntity>() {
			@Override
			public ComplexEntity map(ComplexEntity value) throws Exception {

				if (value != null) {
					if (value.fields != null && value.fields.containsKey("area")) {
						assert value.geomType == Type.Polyline;
						Polyline polyline = (Polyline) GeometryEngine.geometryFromEsriShape(value.shapeGeometry,
								value.geomType);
						Polygon p = new Polygon();
						p.add(polyline, false);

						value.shapeGeometry = GeometryEngine.geometryToEsriShape(p);
						value.geomType = Type.Polygon;

					}
				}

				return value;
			}

		});

		DataSet<Relation> retRelations = rels.filter(new FilterFunction<Relation>() {
			@Override
			public boolean filter(Relation value) throws Exception {

				if (value == null)
					return false;

				assert value != null;
				for (RelatedObject r : value.relatedObjects) {
					if (("inner".equals(r.role) || "outer".equals(r.role)) && "way".equals(r.type)) {
						return false;
					}
				}

				return true;
			}
		});

		// handle the relations and polygons
		DataSet<Tuple4<Long, Long, Integer, Role>> relsPolygon = rels
				.flatMap(new FlatMapFunction<Relation, Tuple4<Long, Long, Integer, Role>>() {
					@Override
					public void flatMap(Relation value, Collector<Tuple4<Long, Long, Integer, Role>> out)
							throws Exception {
						if (value.relatedObjects != null) {

							// if this is a polygon relation, emit the elements

							int c = 0;
							for (RelatedObject r : value.relatedObjects) {
								if (("inner".equals(r.role) || "outer".equals(r.role)) && "way".equals(r.type)) {
									Role role = Role.OUTER;
									if ("inner".equals(r.role)) {
										role = Role.INNER;
									}
									out.collect(new Tuple4<>(value.id, r.relatedId, c++, role));
								}
							}

						}
					}

				});

		DataSet<Tuple4<Long, Integer, Role, byte[]>> joinedWaysForPolygonConstruct = relsPolygon.join(waysGeometry)
				.where(1).equalTo(0).projectFirst(0, 2, 3).projectSecond(1);

		// joinedWays : id, order, role, byte[]
		DataSet<Tuple2<Long, byte[]>> constructedPolygons = joinedWaysForPolygonConstruct.groupBy(0)
				.sortGroup(1, Order.ASCENDING)
				.reduceGroup(new GroupReduceFunction<Tuple4<Long, Integer, Role, byte[]>, Tuple2<Long, byte[]>>() {
					@Override
					public void reduce(Iterable<Tuple4<Long, Integer, Role, byte[]>> values,
							Collector<Tuple2<Long, byte[]>> out) throws Exception {

						ArrayList<MultiPath> polys = new ArrayList<>();
						ArrayList<Role> roles = new ArrayList<>();
						long id = -1;
						for (Tuple4<Long, Integer, Role, byte[]> e : values) {
							id = e.f0;
							MultiPath g = (MultiPath) GeometryEngine.geometryFromEsriShape(e.f3,
									Geometry.Type.Polyline);
							polys.add(g);
							roles.add(e.f2);
						}

						try {
							Polygon polygon = PolygonCreator.createPolygon(polys.toArray(new MultiPath[polys.size()]),
									roles.toArray(new Role[roles.size()]));

							out.collect(new Tuple2<>(id, GeometryEngine.geometryToEsriShape(polygon)));

						} catch (Exception ex) {
							System.out.println("Error creating polygon " + id);
							// ex.printStackTrace();
						}
					}
				});

		DataSet<ComplexEntity> retPolygons = rels.join(constructedPolygons).where(new KeySelector<Relation, Long>() {
			@Override
			public Long getKey(Relation value) throws Exception {
				return value.id;
			}
		}).equalTo(0).with(new JoinFunction<Relation, Tuple2<Long, byte[]>, ComplexEntity>() {
			@Override
			public ComplexEntity join(Relation first, Tuple2<Long, byte[]> second) throws Exception {
				ComplexEntity ce = new ComplexEntity();
				ce.id = first.id;
				ce.fields = first.fields;
				ce.geomType = Type.Polygon;
				ce.shapeGeometry = second.f1;
				return ce;
			}
		});

		OSMResultsStreams rs = new OSMResultsStreams();
		rs.retNodesWithAttributes = retNodesWithAttributes;
		rs.retPolygons = retPolygons.union(waysAndPolys.filter(new FilterFunction<ComplexEntity>() {
			@Override
			public boolean filter(ComplexEntity value) throws Exception {
				if (value == null)
					return false;

				if (value.geomType == Type.Polygon)
					return true;
				return false;
			}
		}));
		rs.retRelations = retRelations;
		
		
		rs.retWaysEntities = waysAndPolys.filter(new FilterFunction<ComplexEntity>() {
			@Override
			public boolean filter(ComplexEntity value) throws Exception {
				if (value == null)
					return false;

				if (value.geomType == Type.Polyline)
					return true;
				return false;
			}
		});

		return rs;

	}

	public static void main(String[] args) throws Exception {

		File resultFolder = new File("f:\\temp\\testfolder2");
		if (!resultFolder.exists()) {
			assert resultFolder.mkdirs();
		}

		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

		// String file = "C:/projets/OSMImport/france-latest.osm.pbf";
		String file = "F:/temp/france-latest.osm.pbf";
		// String file = "C:/projets/OSMImport/rhone-alpes-latest.osm.pbf";

		OSMResultsStreams rs = constructOSMStreams(env, file);

		rs.retNodesWithAttributes.map(new MapFunction<NodeEntity, Tuple4<Long, Double, Double, String>>() {
			@Override
			public Tuple4<Long, Double, Double, String> map(NodeEntity value) throws Exception {
				return new Tuple4<>(value.id, value.x, value.y, convertToString(value.fields));
			}
		}).writeAsCsv(new File(resultFolder, "nodes.csv").getAbsolutePath());

		rs.retPolygons.map(new MapFunction<ComplexEntity, Tuple3<Long, String, String>>() {
			@Override
			public Tuple3<Long, String, String> map(ComplexEntity value) throws Exception {
				return new Tuple3<>(value.id, GeometryTools.toAscii(value.shapeGeometry),
						convertToString(value.fields));
			}
		}).writeAsCsv(new File(resultFolder, "polygons.csv").getAbsolutePath());

		rs.retWaysEntities.map(new MapFunction<ComplexEntity, Tuple3<Long, String, String>>() {
			@Override
			public Tuple3<Long, String, String> map(ComplexEntity value) throws Exception {
				return new Tuple3<>(value.id, GeometryTools.toAscii(value.shapeGeometry),
						convertToString(value.fields));
			}

		}).writeAsCsv(new File(resultFolder, "ways.csv").getAbsolutePath());

		rs.retRelations.map(new MapFunction<Relation, Tuple2<Long, String>>() {
			@Override
			public Tuple2<Long, String> map(Relation value) throws Exception {
				return new Tuple2<>(value.id, convertToString(value.fields));
			}

		}).writeAsCsv(new File(resultFolder, "rels.csv").getAbsolutePath());

		env.execute();
	}

}
