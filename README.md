# Osm-Flink-Tools

Tools for integrating OSM data in HDFS or CSV files.

use the **ESRI-Geometry Hadoop tools** for efficient **ser/deser** 

This project provide inputFormat for reading PBF OSM files and create lines, polygons, relations, permitting to use OSM data in big data chains

Polylines (Ways) Polygons are reconstructed, for a direct use, ESRI-Geometry provide geometry primitives for buffers, intersections, quadtree .. etc

This project use flink as the main framework, as this is built on top of MapReduce object, this can be ported to the standard Spark or Hadoop framwork.


##Usage


ProcessOSM class, that can be used as a single process (Yarn or flink cluster):



	public static void main(String[] args) throws Exception {

		if (args.length < 2)
			throw new Exception("not enought parameters");
		String inputPbf = args[0];

		System.out.println(" input pbf :" + inputPbf);

		String outputResultFolder = args[1];
		System.out.println(" output result folder :" + outputResultFolder);

		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

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
						HashMap<String, Object> h = new HashMap<>();
						h.put("relid", r.relatedId);
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

		env.execute();
	}


