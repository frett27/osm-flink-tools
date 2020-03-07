package org.frett27.streamtests;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.AvroInputFormat;
import org.apache.flink.core.fs.Path;
import org.apache.flink.util.Collector;
import org.osm.avro.OSMEntity;

import junit.framework.TestCase;

public class TestReadAVRO extends TestCase {

  public void testReadAVRO() throws Exception {

    ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
    AvroInputFormat<OSMEntity> f =
        new AvroInputFormat<>(new Path("C:/temp/outputosmavro/avro"), OSMEntity.class);

    env.readFile(f, "C:/temp/outputosmavro/avro")
        .flatMap(
            new FlatMapFunction<OSMEntity, Void>() {
              @Override
              public void flatMap(OSMEntity value, Collector<Void> out) throws Exception {}
            })
        .print();
  }
}
