package org.frett27.streamtests;

import org.apache.flink.api.java.ExecutionEnvironment;
import org.frett27.spatialflink.inputs.process.ProcessOSM;
import org.frett27.spatialflink.inputs.process.ProcessOSM.OSMResultsStreams;

import junit.framework.TestCase;

public class TestReadS3Streams extends TestCase {

	public void testReadStreams() throws Exception {

		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();

		OSMResultsStreams streams = ProcessOSM.constructOSMStreamsFrom(env, "s3://workfilesosm");

		streams.retNodesWithAttributes.count();

	}

}
