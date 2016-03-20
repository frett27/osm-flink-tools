package org.frett27.spatialflink.tools;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class TestMapStringTools extends TestCase {

	public void testConvertToString() throws Exception {
		Map<String, Object> t = new HashMap<String, Object>();
		t.put("hello", "hello");

		System.out.println(MapStringTools.convertToString(t));

	}

}
