package org.frett27.spatialflink.tools;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class TestMapStringTools extends TestCase {

	public String[][] TEST_CASES = {
		new String[]{}	
	};
	
	
	
	public void testConvertToString() throws Exception {
		
		Map<String, String> t = new HashMap<>();
	
		System.out.println(MapStringTools.convertToString(t));

	}

	
	
	
}
