package org.frett27.streamtests;

import java.io.File;

import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.LocalEnvironment;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.GlobalConfiguration;
import org.frett27.spatialflink.inputs.process.ProcessOSM;
import org.frett27.spatialflink.inputs.process.ProcessOSM.OSMResultsStreams;

public class TestReadCSVFolder {

	public static void main(String[] args) throws Exception {

		Configuration config = new Configuration();
		config.setInteger("delimited-format.numSamples", 10000);
		
		LocalEnvironment env = ExecutionEnvironment.createLocalEnvironment(config);
		

		OSMResultsStreams s = ProcessOSM.constructOSMStreamsFromFolder(env,
				new File("C:\\projets\\Hydrant_Spatial_Analysis\\dev\\Analysis\\processedData"));

		//System.out.println(s.retNodesWithAttributes.first(10).collect());
		System.out.println(s.retPolygons.first(10).collect());
		// System.out.println(s.retWaysEntities.first(10).collect());
		//System.out.println(s.retRelations.first(10).collect());
		

	}

}
