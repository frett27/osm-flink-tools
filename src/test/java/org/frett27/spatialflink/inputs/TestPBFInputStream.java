package org.frett27.spatialflink.inputs;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.typeutils.GenericTypeInfo;
import org.apache.flink.core.fs.FileInputSplit;
import org.apache.flink.util.Collector;
import org.frett27.spatialflink.model.NodeEntity;

public class TestPBFInputStream {

	public static void main(String[] argv) throws Exception {

		// BasicConfigurator.configure(new ConsoleAppender(new
		// PatternLayout()));

		ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment();

		OSMPBFNodeInputFormat iformat = new OSMPBFNodeInputFormat();

		iformat.setFilePath("C:/projets/OSMImport/rhone-alpes-latest.osm.pbf");

		// iformat.setFilePath("C:/projets/OSMImport/rhone-alpes-latest.osm.pbf");

		FileInputSplit[] s = iformat.createInputSplits(4);

		DataSource<NodeEntity> r = env.createInput(iformat, new GenericTypeInfo<NodeEntity>(NodeEntity.class));
		System.out.println(r.flatMap(new FlatMapFunction<NodeEntity, Tuple4<Long, String, Double, Double>>() {
			@Override
			public void flatMap(NodeEntity value, Collector<Tuple4<Long, String, Double, Double>> out)
					throws Exception {
				if (value.fields != null) {
					if (value.fields.containsKey("type")) {
						out.collect(new Tuple4<>(value.id, (String) value.fields.get("type"), value.x, value.y));
					}
				}
			}
		}).count()); // writeAsCsv("test.csv");

		//env.execute();

	}

}
