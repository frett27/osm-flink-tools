package org.frett27.spatialflink.tools;

import junit.framework.TestCase;

public class TestStringPop extends TestCase {

	public void testS1() throws Exception {
		StringBuilder s = new StringBuilder("test1,test2");
		String r = StringPop.pop(s, ",");

		assertEquals("test1", r);
		assertEquals("test2", s.toString());
	}

	/**
	 * more characters for delimiters
	 * 
	 * @throws Exception
	 */
	public void testS2() throws Exception {
		StringBuilder s = new StringBuilder("test1,,test2");
		String r = StringPop.pop(s, ",,");

		assertEquals("test1", r);
		assertEquals("test2", s.toString());
	}

	/**
	 * no delimiters
	 * 
	 * @throws Exception
	 */
	public void testS3() throws Exception {
		StringBuilder s = new StringBuilder("test1,,test2");
		String r = StringPop.pop(s, "|");

		assertEquals("test1,,test2", r);
		assertEquals("", s.toString());
	}

	/**
	 * no delimiters
	 * 
	 * @throws Exception
	 */
	public void testS4() throws Exception {
		StringBuilder s = new StringBuilder("test1,,test2");
		String r = StringPop.pop(s, ",");

		assertEquals("test1", r);
		assertEquals(",test2", s.toString());

		r = StringPop.pop(s, ",");

		assertEquals("", r);
		assertEquals("test2", s.toString());

		r = StringPop.pop(s, ",");

		assertEquals("test2", r);
		assertEquals("", s.toString());

	}

}
