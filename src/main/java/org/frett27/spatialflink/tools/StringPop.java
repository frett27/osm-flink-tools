package org.frett27.spatialflink.tools;

public class StringPop {

	public static String pop(StringBuilder s, String delimiter) {
		int indexOf = s.indexOf(delimiter);
		if (indexOf != -1) {
			String ret = s.substring(0, indexOf);
			s.delete(0, indexOf + delimiter.length());
			return ret;
		}
		String ret = s.toString();
		s.delete(0, s.length());
		return ret;
	}

}
