package org.frett27.spatialflink.inputs;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.GenericTypeInfo;
import org.apache.flink.core.fs.FileInputSplit;
import org.apache.flink.util.Collector;
import org.frett27.spatialflink.model.Relation;
import org.frett27.spatialflink.model.WayEntity;

public class TestPBFWayInputStream {

	public static void main(String[] argv) throws Exception {

		// BasicConfigurator.configure(new ConsoleAppender(new
		// PatternLayout()));

		ExecutionEnvironment env = ExecutionEnvironment.createLocalEnvironment();

		OSMPBFWayInputFormat iformat = new OSMPBFWayInputFormat();

		iformat.setFilePath("C:/projets/OSMImport/france-latest.osm.pbf");

		// iformat.setFilePath("C:/projets/OSMImport/rhone-alpes-latest.osm.pbf");

		FileInputSplit[] s = iformat.createInputSplits(4);

		DataSource<WayEntity> r = env.createInput(iformat, new GenericTypeInfo<WayEntity>(WayEntity.class));
		r.flatMap(new FlatMapFunction<WayEntity, Tuple2<Long, String>>() {
			@Override
			public void flatMap(WayEntity value, Collector<Tuple2<Long, String>> out)
					throws Exception {
				if (value.fields != null) {
					if (value.fields.containsKey("type")) {
						out.collect(new Tuple2<>(value.id, (String) value.fields.get("type")));
					}
				}
			}
		}).writeAsCsv("test.csv");

		env.execute();

	}

}
