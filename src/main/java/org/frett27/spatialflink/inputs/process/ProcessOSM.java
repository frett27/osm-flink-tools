package org.frett27.spatialflink.inputs.process;

import java.util.ArrayList;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.GroupReduceFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.operators.FlatMapOperator;
import org.apache.flink.api.java.operators.MapOperator;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.typeutils.GenericTypeInfo;
import org.apache.flink.util.Collector;
import org.frett27.spatialflink.inputs.OSMPBFNodeInputFormat;
import org.frett27.spatialflink.inputs.OSMPBFRelationInputFormat;
import org.frett27.spatialflink.inputs.OSMPBFWayInputFormat;
import org.frett27.spatialflink.model.NodeEntity;
import org.frett27.spatialflink.model.RelatedObject;
import org.frett27.spatialflink.model.Relation;
import org.frett27.spatialflink.tools.PolygonCreator;
import org.frett27.spatialflink.tools.PolygonCreator.Role;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;

public class ProcessOSM {

	public static void main(String[] args) throws Exception {

		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

		String file = "C:/projets/OSMImport/france-latest.osm.pbf";
		// String file = "C:/projets/OSMImport/rhone-alpes-latest.osm.pbf";

		OSMPBFNodeInputFormat iformat = new OSMPBFNodeInputFormat();
		iformat.setFilePath(file);
		DataSource<NodeEntity> nodes = env.createInput(iformat, new GenericTypeInfo<NodeEntity>(NodeEntity.class));

		// get only the positions of the nodes
		MapOperator<NodeEntity, Tuple3<Long, Double, Double>> onlypos = nodes
				.map(new MapFunction<NodeEntity, Tuple3<Long, Double, Double>>() {
					@Override
					public Tuple3<Long, Double, Double> map(NodeEntity value) throws Exception {
						return new Tuple3<Long, Double, Double>(value.id, value.x, value.y);
					}
				});

		// read the ways
		OSMPBFWayInputFormat iformatw = new OSMPBFWayInputFormat();
		iformatw.setFilePath(file);
		DataSource<Relation> ways = env.createInput(iformatw, new GenericTypeInfo<Relation>(Relation.class));

		FlatMapOperator<Relation, Tuple3<Long, Long, Integer>> relsLink = ways
				.flatMap(new FlatMapFunction<Relation, Tuple3<Long, Long, Integer>>() {
					@Override
					public void flatMap(Relation value, Collector<Tuple3<Long, Long, Integer>> out) throws Exception {
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

		DataSet<Tuple4<Long, Integer, Double, Double>> joinedWaysWithPoints = relsLink.join(onlypos).where(1).equalTo(0)
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

		// .writeAsCsv("constructedPolys.csv");

		OSMPBFRelationInputFormat iformatrels = new OSMPBFRelationInputFormat();
		iformatrels.setFilePath(file);
		DataSource<Relation> rels = env.createInput(iformatrels, new GenericTypeInfo<Relation>(Relation.class));

		// handle the relations and polygons
		FlatMapOperator<Relation, Tuple4<Long, Long, Integer, Role>> relsPolygon = rels
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

		DataSet<Tuple2<Long, byte[]>> ret = joinedWaysForPolygonConstruct.groupBy(0).sortGroup(1, Order.ASCENDING)
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

		ret.writeAsCsv("polys.csv");

		// .reduceGroup(new GroupReduceFunction<Tuple3<Long, Integer, byte[]>,
		// String>() {
		// @Override
		// public void reduce(Iterable<Tuple3<Long, Integer, byte[]>> values,
		// Collector<String> out) throws Exception {
		//
		//
		// }
		// });// .write("result.csv");
		//

		env.execute();
	}

}
