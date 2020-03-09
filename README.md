# Osm-Flink-Tools

![](https://travis-ci.org/frett27/osm-flink-tools.svg?branch=master)

Tools for integrating big pbf OSM data files in CSV or Avro files, with the geometry reconstructed. (lines, polygons).


## Goal - Ease the use of OSM datas in flink / Hadoop clusters

This project provides inputFormat for reading PBF OSM files and create lines, polygons, relations, permitting to use OSM data in big data stacks.

Polylines (Ways) Polygons are reconstructed, for a direct use, ESRI-Geometry provide geometry primitives for buffers, intersections, quadtree .. etc

This project uses flink as the main framework, as this is built on top of MapReduce object, this can be ported to the standard Spark or Hadoop framwork.

## Design And Output file format


we use the **ESRI-Geometry java API** [https://github.com/Esri/geometry-api-java](https://github.com/Esri/geometry-api-java) for efficient **ser/deser** 


a detailed explanation of the output file format is described Here : [Output File Format Description](doc/exported_file_format.md)



## Using the tool as a standalone command line (5 mins startup)

This tool can now be used as a single commandline, as well as a job on a cluster.

Using the jar as a standalone jar :

__be sure you have at least java 7 or 8 in the path__

	java -jar osm-flink-tools-[version]-all.jar -in rhone-alpes-latest.osm.pbf -out .\


this command line will create 4 folders containing the reconstructed geometries , as described here : [Output File Format Description](doc/exported_file_format.md)

if you wish to generate utf-8 or local specific csv files, add `-Dfile.encoding=UTF-8` in the command line to use the given charset in the output files, this helps integrate worldwide files or foreign countries. (this must be specified before the -jar option)


## Using the Tool in a flink cluster or flink Job


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


