package org.frett27.spatialflink.inputs.process;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.AvroOutputFormat;
import org.frett27.spatialflink.inputs.process.ProcessOSM.OSMResultsStreams;
import org.frett27.spatialflink.model.ComplexEntity;
import org.frett27.spatialflink.model.NodeEntity;
import org.frett27.spatialflink.model.RelatedObject;
import org.frett27.spatialflink.model.Relation;
import org.osm.avro.AComplex;
import org.osm.avro.ANode;
import org.osm.avro.ARelated;
import org.osm.avro.ARelation;
import org.osm.avro.OSMEntity;
import org.osm.avro.OSMType;

public class ProcessOSMAvro {

	public static void main(String[] args) throws Exception {

		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

		Options options = new Options();

		Option inputOption = new Option("in", "inputPbf", true, "Protocolbuffer input file");
		inputOption.setRequired(true);
		options.addOption(inputOption);

		Option outputOption = new Option("out", "outputResultFolder", true, "output result folder");
		outputOption.setRequired(true);
		options.addOption(outputOption);

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
			formatter.printHelp("processosm", options);
			System.exit(1);
		}

		if (!line.hasOption("in")) {
			throw new Exception("must pass input");
		}

		String inputPbf = line.getOptionValue("in");

		if (!line.hasOption("out")) {
			throw new Exception("must pass output");

		}

		String outputResult = line.getOptionValue("out");

		System.out.println(" input pbf :" + inputPbf);

		System.out.println(" output result  :" + outputResult);

		// String file = "C:/projets/OSMImport/rhone-alpes-latest.osm.pbf";
		// String file = "F:/temp/france-latest.osm.pbf";
		// String file = "C:/projets/OSMImport/rhone-alpes-latest.osm.pbf";

		OSMResultsStreams rs = ProcessOSM.constructOSMStreams(env, inputPbf);

		DataSet<OSMEntity> f1 = rs.retNodesWithAttributes.map(new MapFunction<NodeEntity, OSMEntity>() {
			public OSMEntity map(NodeEntity value) throws Exception {
				ANode ret = new ANode();
				ret.setFields(value.fields);
				ret.setId(value.id);
				ret.setX(value.x);
				ret.setY(value.y);

				return new OSMEntity(OSMType.NODE, value.id, ret, null, null, null);
			};
		});

		DataSet<OSMEntity> f2 = rs.retPolygons.map(new MapFunction<ComplexEntity, OSMEntity>() {
			public OSMEntity map(ComplexEntity value) throws Exception {

				AComplex c = new AComplex(value.id, ByteBuffer.wrap(value.shapeGeometry), value.fields);

				return new OSMEntity(OSMType.POLYGON, value.id, null, null, c, null);
			};
		});

		DataSet<OSMEntity> f3 = rs.retWaysEntities.map(new MapFunction<ComplexEntity, OSMEntity>() {
			@Override
			public OSMEntity map(ComplexEntity value) throws Exception {
				AComplex c = new AComplex(value.id, ByteBuffer.wrap(value.shapeGeometry), value.fields);

				return new OSMEntity(OSMType.WAY, value.id, null, c, null, null);
			}
		});

		DataSet<OSMEntity> f4 = rs.retRelations.map(new MapFunction<Relation, OSMEntity>() {
			public OSMEntity map(Relation value) {

				ArrayList<ARelated> related = new ArrayList<ARelated>();
				// grab all the relations
				if (value.relatedObjects != null) {
					for (RelatedObject o : value.relatedObjects) {
						related.add(new ARelated(o.relatedId, o.type, o.role));
					}
				}

				ARelation r = new ARelation(value.id, value.fields, related);

				return new OSMEntity(OSMType.RELATION, value.id, null, null, null, r);
			};
		});

		AvroOutputFormat<OSMEntity> avro = new AvroOutputFormat<OSMEntity>(OSMEntity.class);

		f1.union(f2).union(f3).union(f4).write(avro, outputResult);

		env.execute();

	}
}
