package org.frett27.flinktests;

import org.apache.flink.api.java.typeutils.TypeExtractor;
import org.frett27.spatialflink.model.NodeEntity;

public class TestType {

	public static void main(String[] args) {
		
		System.out.println(TypeExtractor.createTypeInfo(NodeEntity.class));
		
	}

}
