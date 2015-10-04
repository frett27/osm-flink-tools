package org.frett27.spatialflink.inputs.process;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.GroupReduceFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.operators.FlatMapOperator;
import org.apache.flink.api.java.operators.MapOperator;
import org.apache.flink.api.java.tuple.Tuple1;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.api.java.typeutils.GenericTypeInfo;
import org.apache.flink.util.Collector;
import org.frett27.spatialflink.inputs.OSMPBFNodeInputFormat;
import org.frett27.spatialflink.inputs.OSMPBFWayInputFormat;
import org.frett27.spatialflink.model.NodeEntity;
import org.frett27.spatialflink.model.RelatedObject;
import org.frett27.spatialflink.model.Relation;

public class ProcessOSM {

	public static void main(String[] args) throws Exception {

		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

		String file = "C:/projets/OSMImport/france-latest.osm.pbf";
		// String file = "C:/projets/OSMImport/rhone-alpes-latest.osm.pbf";
		
		OSMPBFNodeInputFormat iformat = new OSMPBFNodeInputFormat();
		iformat.setFilePath(file);
		DataSource<NodeEntity> nodes = env.createInput(iformat, new GenericTypeInfo<NodeEntity>(NodeEntity.class));

		MapOperator<NodeEntity, Tuple3<Long, Double, Double>> onlypos = nodes
				.map(new MapFunction<NodeEntity, Tuple3<Long, Double, Double>>() {
					@Override
					public Tuple3<Long, Double, Double> map(NodeEntity value) throws Exception {
						return new Tuple3<Long, Double, Double>(value.id, value.x, value.y);
					}
				});

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

		DataSet<Tuple4<Long, Integer, Double, Double>> joined = relsLink.join(onlypos).where(1).equalTo(0)
				.projectFirst(0, 2).projectSecond(1, 2);

		joined.groupBy(0).sortGroup(1, Order.ASCENDING)
				.reduceGroup(new GroupReduceFunction<Tuple4<Long, Integer, Double, Double>, Tuple1<String>>() {
					@Override
					public void reduce(Iterable<Tuple4<Long, Integer, Double, Double>> values, Collector<Tuple1<String>> out)
							throws Exception {
						long id = -1;
						StringBuilder sb = new StringBuilder();
						for (Tuple4<Long, Integer, Double, Double> t : values) {
							id = t.getField(0);
							double x = t.getField(2);
							double y = t.getField(3);
							if (sb.length() > 0)
								sb.append(",");
							sb.append("(").append(x).append(",").append(y).append(")");

						}
						sb.insert(0, "id :" + id);
						out.collect(new Tuple1<String>(sb.toString()));
					}
				}).writeAsCsv("constructedPolys.csv");

		env.execute();
	}

}


