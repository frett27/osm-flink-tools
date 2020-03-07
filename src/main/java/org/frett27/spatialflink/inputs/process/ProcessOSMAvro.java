package org.frett27.spatialflink.inputs.process;

import java.nio.ByteBuffer;
import java.util.ArrayList;

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

	if (args.length < 2)
		throw new Exception("not enought parameters");
	String inputPbf = args[0];

	System.out.println(" input pbf :" + inputPbf);

	String outputResult = args[1];
	System.out.println(" output result  :" + outputResult);

    // String file = "C:/projets/OSMImport/rhone-alpes-latest.osm.pbf";
    // String file = "F:/temp/france-latest.osm.pbf";
    // String file = "C:/projets/OSMImport/rhone-alpes-latest.osm.pbf";

    OSMResultsStreams rs = ProcessOSM.constructOSMStreams(env, inputPbf);

    DataSet<OSMEntity> f1 =
        rs.retNodesWithAttributes.map(
            new MapFunction<NodeEntity, OSMEntity>() {
              public OSMEntity map(NodeEntity value) throws Exception {
                ANode ret = new ANode();
                ret.setFields(value.fields);
                ret.setId(value.id);
                ret.setX(value.x);
                ret.setY(value.y);

                return new OSMEntity(OSMType.NODE, value.id, ret, null, null, null);
              };
            });

    DataSet<OSMEntity> f2 =
        rs.retPolygons.map(
            new MapFunction<ComplexEntity, OSMEntity>() {
              public OSMEntity map(ComplexEntity value) throws Exception {

                AComplex c =
                    new AComplex(value.id, ByteBuffer.wrap(value.shapeGeometry), value.fields);

                return new OSMEntity(OSMType.POLYGON, value.id, null, null, c, null);
              };
            });

    DataSet<OSMEntity> f3 =
        rs.retWaysEntities.map(
            new MapFunction<ComplexEntity, OSMEntity>() {
              @Override
              public OSMEntity map(ComplexEntity value) throws Exception {
                AComplex c =
                    new AComplex(value.id, ByteBuffer.wrap(value.shapeGeometry), value.fields);

                return new OSMEntity(OSMType.WAY, value.id, null, c, null, null);
              }
            });

    DataSet<OSMEntity> f4 =
        rs.retRelations.map(
            new MapFunction<Relation, OSMEntity>() {
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
