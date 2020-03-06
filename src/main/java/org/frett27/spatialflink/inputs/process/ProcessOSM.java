package org.frett27.spatialflink.inputs.process;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.flink.api.common.functions.FilterFunction;
import org.apache.flink.api.common.functions.FlatJoinFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.GroupReduceFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.typeutils.GenericTypeInfo;
import org.apache.flink.util.Collector;
import org.frett27.spatialflink.inputs.OSMPBFNodeInputFormat;
import org.frett27.spatialflink.inputs.OSMPBFRelationInputFormat;
import org.frett27.spatialflink.inputs.OSMPBFWayInputFormat;
import org.frett27.spatialflink.model.ComplexEntity;
import org.frett27.spatialflink.model.NodeEntity;
import org.frett27.spatialflink.model.RelatedObject;
import org.frett27.spatialflink.model.Relation;
import org.frett27.spatialflink.model.WayEntity;
import org.frett27.spatialflink.tools.GeometryTools;
import org.frett27.spatialflink.tools.MapStringTools;
import org.frett27.spatialflink.tools.PolygonCreator;
import org.frett27.spatialflink.tools.Role;
import org.frett27.spatialflink.tools.StringPop;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Geometry.Type;
import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Point;
import com.esri.core.geometry.Polygon;
import com.esri.core.geometry.Polyline;

public class ProcessOSM {

	public static class OSMResultsStreams {

		public DataSet<ComplexEntity> retWaysEntities;
		public DataSet<ComplexEntity> retPolygons;
		public DataSet<NodeEntity> retNodesWithAttributes;
		public DataSet<Relation> retRelations;

	}

	////////////////////////////////////////////////////////////////////////////////////////////
	// construct osm stream from pbf
	
	public static OSMResultsStreams constructOSMStreams(ExecutionEnvironment env, String inputFile) throws Exception {

		OSMPBFNodeInputFormat iNodes = new OSMPBFNodeInputFormat();
		iNodes.setFilePath(inputFile);
		DataSet<NodeEntity> nodes = env.createInput(iNodes, new GenericTypeInfo<NodeEntity>(NodeEntity.class));

		OSMPBFWayInputFormat iWays = new OSMPBFWayInputFormat();
		iWays.setFilePath(inputFile);
		DataSet<WayEntity> ways = env.createInput(iWays, new GenericTypeInfo<WayEntity>(WayEntity.class));

		OSMPBFRelationInputFormat iRelation = new OSMPBFRelationInputFormat();
		iRelation.setFilePath(inputFile);
		DataSet<Relation> rels = env.createInput(iRelation, new GenericTypeInfo<Relation>(Relation.class));

		// get only the positions of the nodes
		DataSet<Tuple3<Long, Double, Double>> onlypos = nodes
				.map(new MapFunction<NodeEntity, Tuple3<Long, Double, Double>>() {
					@Override
					public Tuple3<Long, Double, Double> map(NodeEntity value) throws Exception {
						return new Tuple3<Long, Double, Double>(value.id, value.x, value.y);
					}
				}).sortPartition(0, Order.ASCENDING);

		DataSet<NodeEntity> retNodesWithAttributes = nodes.filter(new FilterFunction<NodeEntity>() {
			@Override
			public boolean filter(NodeEntity value) throws Exception {
				if (value == null)
					return false;
				return value.fields != null && value.fields.size() > 0;
			}
		});

		DataSet<Tuple3<Long, Long, Integer>> relsLink = ways
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
				}).sortPartition(1, Order.ASCENDING);

		// relslink contains id, related and order
		// only pos contains id, x, y
		DataSet<Tuple4<Long, Integer, Double, Double>> joinedWaysWithPoints = relsLink.joinWithHuge(onlypos).where(1)
				.equalTo(0).projectFirst(0, 2).projectSecond(1, 2);

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
		DataSet<ComplexEntity> retWaysEntities = ways.join(waysGeometry)
				.where(new KeySelector<WayEntity, Long>() {
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

				if (value == null || value.relatedObjects == null)
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

								if ("way".equals(r.type)) {
									Role role = Role.UNDEFINED;
									if ("inner".equals(r.role)) {
										role = Role.INNER;
									} else if ("outer".equals(r.role)) {
										role = Role.OUTER;
									}
									out.collect(new Tuple4<>(value.id, r.relatedId, c++, role));
								}
							}

						}
					}

				});

		DataSet<Tuple4<Long, Integer, Role, byte[]>> joinedWaysForPolygonConstruct = relsPolygon
				.join(waysGeometry).where(1).equalTo(0).projectFirst(0, 2, 3).projectSecond(1);

		// joinedWays : id, order, role, byte[]
		DataSet<Tuple2<Long, byte[]>> constructedPolygons = joinedWaysForPolygonConstruct.groupBy(0)
				.sortGroup(1, Order.ASCENDING)
				.reduceGroup(new GroupReduceFunction<Tuple4<Long, Integer, Role, byte[]>, Tuple2<Long, byte[]>>() {
					@Override
					public void reduce(Iterable<Tuple4<Long, Integer, Role, byte[]>> values,
							Collector<Tuple2<Long, byte[]>> out) throws Exception {

						if (values == null)
							return;

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

		// joins with attributes

		DataSet<ComplexEntity> retPolygons = rels.join(constructedPolygons)
				.where(new KeySelector<Relation, Long>() {
					@Override
					public Long getKey(Relation value) throws Exception {
						return value.id;
					}
				}).equalTo(0).with(new FlatJoinFunction<Relation, Tuple2<Long, byte[]>, ComplexEntity>() {

					@Override
					public void join(Relation first, Tuple2<Long, byte[]> second, Collector<ComplexEntity> out)
							throws Exception {
						if (first == null || second == null)
							return;

						ComplexEntity ce = new ComplexEntity();
						ce.id = first.id;
						ce.fields = first.fields;
						ce.geomType = Type.Polygon;
						ce.shapeGeometry = second.f1;
						out.collect(ce);
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
		}); // .returns("DataSet<ComplexEntity>");

		return rs;

	}

	/**
	 * construct the streams from HDFS, S3
	 * 
	 * @param env
	 * @param reference
	 * @return
	 * @throws Exception
	 */
	public static OSMResultsStreams constructOSMStreamsFrom(ExecutionEnvironment env, String path) throws Exception {

		DataSet<Tuple4<Long, Double, Double, String>> nodesDataset = env.readTextFile(path + "/nodes.csv")
				.map(new MapFunction<String, Tuple4<Long, Double, Double, String>>() {
					@Override
					public Tuple4<Long, Double, Double, String> map(String value) throws Exception {

						StringBuilder v = new StringBuilder(value);
						return new Tuple4<Long, Double, Double, String>(Long.parseLong(StringPop.pop(v, ",")),
								Double.parseDouble(StringPop.pop(v, ",")), Double.parseDouble(StringPop.pop(v, ",")),
								StringPop.pop(v, ","));

					}
				});

		DataSet<Tuple3<Long, String, String>> polygonsDataset = env.readTextFile(path + "/polygons.csv")
				.map(new MapFunction<String, Tuple3<Long, String, String>>() {

					@Override
					public Tuple3<Long, String, String> map(String value) throws Exception {
						StringBuilder v = new StringBuilder(value);
						return new Tuple3<Long, String, String>(Long.parseLong(StringPop.pop(v, ",")),
								StringPop.pop(v, ","), StringPop.pop(v, ","));
					}
				});

		DataSet<Tuple3<Long, String, String>> waysDataset = env.readTextFile(path + "/ways.csv")
				.map(new MapFunction<String, Tuple3<Long, String, String>>() {

					@Override
					public Tuple3<Long, String, String> map(String value) throws Exception {
						StringBuilder v = new StringBuilder(value);
						return new Tuple3<Long, String, String>(Long.parseLong(StringPop.pop(v, ",")),
								StringPop.pop(v, ","), StringPop.pop(v, ","));
					}
				});

		DataSet<Tuple3<Long, String, String>> relsDataset = env.readTextFile(path + "/rels.csv")
				.map(new MapFunction<String, Tuple3<Long, String, String>>() {
					@Override
					public Tuple3<Long, String, String> map(String value) throws Exception {
						StringBuilder v = new StringBuilder(value);
						return new Tuple3<Long, String, String>(Long.parseLong(StringPop.pop(v, ",")),
								StringPop.pop(v, ","), StringPop.pop(v, ","));
					}
				});

		return constructOSMStreams(env, nodesDataset, polygonsDataset, waysDataset, relsDataset);
	}

	
	private static OSMResultsStreams constructOSMStreams(ExecutionEnvironment env,
			DataSet<Tuple4<Long, Double, Double, String>> nodesDataset,
			DataSet<Tuple3<Long, String, String>> polygonsDataset, DataSet<Tuple3<Long, String, String>> waysDataset,
			DataSet<Tuple3<Long, String, String>> relsDataset) {

		OSMResultsStreams result = new OSMResultsStreams();

		DataSet<NodeEntity> nodeStream = nodesDataset
				.map(new MapFunction<Tuple4<Long, Double, Double, String>, NodeEntity>() {
					@Override
					public NodeEntity map(Tuple4<Long, Double, Double, String> value) throws Exception {

						NodeEntity nodeEntity = new NodeEntity();
						nodeEntity.id = value.f0;
						nodeEntity.x = value.f1;
						nodeEntity.y = value.f2;
						nodeEntity.fields = MapStringTools.fromString(value.f3);
						return nodeEntity;
					}
				});

		DataSet<ComplexEntity> polygonsStream = polygonsDataset
				.map(new MapFunction<Tuple3<Long, String, String>, ComplexEntity>() {
					@Override
					public ComplexEntity map(Tuple3<Long, String, String> value) throws Exception {

						ComplexEntity polygonEntity = new ComplexEntity();
						polygonEntity.id = value.f0;
						polygonEntity.shapeGeometry = GeometryTools.fromAscii(value.f1);
						polygonEntity.fields = MapStringTools.fromString(value.f2);
						return polygonEntity;
					}
				});

		DataSet<ComplexEntity> waysStream = waysDataset
				.map(new MapFunction<Tuple3<Long, String, String>, ComplexEntity>() {
					@Override
					public ComplexEntity map(Tuple3<Long, String, String> value) throws Exception {

						ComplexEntity waysEntity = new ComplexEntity();
						waysEntity.id = value.f0;
						waysEntity.shapeGeometry = GeometryTools.fromAscii(value.f1);
						waysEntity.fields = MapStringTools.fromString(value.f2);
						return waysEntity;
					}
				});

		DataSet<Relation> relationStream = relsDataset.map(new MapFunction<Tuple3<Long, String, String>, Relation>() {
			@Override
			public Relation map(Tuple3<Long, String, String> value) throws Exception {

				Relation r = new Relation();
				r.id = value.f0;
				r.fields = MapStringTools.fromString(value.f1);

				if (value.f2 != null && !value.f2.isEmpty()) {

					List<RelatedObject> l = new ArrayList<RelatedObject>();

					String[] elements = value.f2.split("||");
					for (String s : elements) {

						if (s == null || s.isEmpty())
							continue;
						Map<String, String> h = MapStringTools.fromString(s);
						if (h == null)
							continue;
						RelatedObject ro = new RelatedObject();

						// h.put("relid", r.relatedId);
						// h.put("role", r.role);
						// h.put("type", r.type);
						//
						if (h.get("relid") == null)
							continue;

						try {
							ro.relatedId = Long.parseLong((String) h.get("relid"));
						} catch (Exception ex) {
							System.out.println("fail to parse :" + h.get("relid"));
						}
						ro.role = (String) h.get("role");
						ro.type = (String) h.get("type");

						l.add(ro);

					}
					r.relatedObjects = l.toArray(new RelatedObject[l.size()]);
				}

				return r;
			}
		});

		result.retNodesWithAttributes = nodeStream;
		result.retPolygons = polygonsStream;
		result.retWaysEntities = waysStream;
		result.retRelations = relationStream;

		// relations

		return result;

	}

	/**
	 * read the csv, and construct streams
	 * 
	 * @param env
	 * @param folder
	 * @return
	 * @throws Exception
	 */
	public static OSMResultsStreams constructOSMStreamsFromFolder(ExecutionEnvironment env, File folder)
			throws Exception {

		File nodes = new File(folder, "nodes.csv");

		DataSet<Tuple4<Long, Double, Double, String>> nodesDataset = env.readCsvFile(nodes.getAbsolutePath())
				.types(Long.class, Double.class, Double.class, String.class);

		File polygons = new File(folder, "polygons.csv");

		DataSet<Tuple3<Long, String, String>> polygonsDataset = env.readCsvFile(polygons.getAbsolutePath())
				.ignoreInvalidLines().types(Long.class, String.class, String.class);

		File ways = new File(folder, "ways.csv");
		DataSet<Tuple3<Long, String, String>> waysDataset = env.readCsvFile(ways.getAbsolutePath()).types(Long.class,
				String.class, String.class);

		File rels = new File(folder, "rels.csv");
		DataSet<Tuple3<Long, String, String>> relsDataset = env.readCsvFile(rels.getAbsolutePath()).types(Long.class,
				String.class, String.class);

		return constructOSMStreams(env, nodesDataset, polygonsDataset, waysDataset, relsDataset);

	}

	public static void main(String[] args) throws Exception {
		
		Options options = new Options();
		
		options.addOption(new Option("in", "inputPbf", true, "Protocolbuffer input file"));
		options.addOption(new Option("out", "outputResultFolder", true, "output result folder"));

		CommandLineParser parser = new DefaultParser();
		CommandLine line = null;
		try {
			// parse the command line arguments
			line = parser.parse(options, args);
		} catch (ParseException exp) {
			// oops, something went wrong
			System.out.println(exp.getMessage());
			// automatically generate the help statement
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "processosm", options );
			System.exit(1);
		}
		
		String inputPbf = null;
		
		if (line.hasOption("in")) {
			inputPbf = line.getOptionValue("in");
		}
		
		assert inputPbf != null;
		
		System.out.println("  Input Pbf file :" + inputPbf);

		File ifile = new File(inputPbf);
		if (!ifile.exists()) {
			throw new Exception("input Pbf file " + ifile + " does not exists");
		}

		String outputResultFolder = null;
		
		if (line.hasOption("out")) {
			outputResultFolder = line.getOptionValue("out");
		}
		
		assert outputResultFolder != null;
		
		System.out.println("  Output result folder :" + outputResultFolder);
		
		File of = new File(outputResultFolder);
		if (!of.exists()) {
			throw new Exception("output result folder " + of + " does not exists");
		}

		// File resultFolder = new File("f:\\temp\\testfolder2");
		// if (!resultFolder.exists()) {
		// assert resultFolder.mkdirs();
		// }

		// Configuration configuration = new Configuration();
		// configuration.setLong("taskmanager.heap.mb", 4000L);
		// configuration.setInteger("taskmanager.numberOfTaskSlots", 4);
		// configuration.setInteger("parallelization.degree.default", 4);
		//
		// ExecutionEnvironment env =
		// ExecutionEnvironment.createLocalEnvironment(configuration);
		//
		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

		// String file = "C:/projets/OSMImport/france-latest.osm.pbf";
		// String file = "F:/temp/france-latest.osm.pbf";
		// String file = "C:/projets/OSMImport/rhone-alpes-latest.osm.pbf";

		OSMResultsStreams rs = constructOSMStreams(env, inputPbf);

		rs.retNodesWithAttributes.map(new MapFunction<NodeEntity, Tuple4<Long, Double, Double, String>>() {
			@Override
			public Tuple4<Long, Double, Double, String> map(NodeEntity value) throws Exception {
				return new Tuple4<>(value.id, value.x, value.y, MapStringTools.convertToString(value.fields));
			}
		}).writeAsCsv(outputResultFolder + "/nodes.csv");

		rs.retPolygons.map(new MapFunction<ComplexEntity, Tuple3<Long, String, String>>() {
			@Override
			public Tuple3<Long, String, String> map(ComplexEntity value) throws Exception {
				return new Tuple3<>(value.id, GeometryTools.toAscii(value.shapeGeometry),
						MapStringTools.convertToString(value.fields));
			}
		}).writeAsCsv(outputResultFolder + "/polygons.csv");

		rs.retWaysEntities.map(new MapFunction<ComplexEntity, Tuple3<Long, String, String>>() {
			@Override
			public Tuple3<Long, String, String> map(ComplexEntity value) throws Exception {
				return new Tuple3<>(value.id, GeometryTools.toAscii(value.shapeGeometry),
						MapStringTools.convertToString(value.fields));
			}

		}).writeAsCsv(outputResultFolder + "/ways.csv");

		rs.retRelations.map(new MapFunction<Relation, Tuple3<Long, String, String>>() {
			@Override
			public Tuple3<Long, String, String> map(Relation value) throws Exception {

				StringBuilder sb = new StringBuilder();
				if (value.relatedObjects != null) {

					for (RelatedObject r : value.relatedObjects) {
						HashMap<String, String> h = new HashMap<>();
						h.put("relid", Long.toString(r.relatedId));
						h.put("role", r.role);
						h.put("type", r.type);
						if (sb.length() > 0) {
							sb.append("||");
						}
						sb.append(MapStringTools.convertToString(h));
					}
				}

				return new Tuple3<>(value.id, MapStringTools.convertToString(value.fields), sb.toString());
			}

		}).writeAsCsv(outputResultFolder + "/rels.csv");

		// System.out.println(env.getExecutionPlan());
		env.execute();
	}

}

