package org.frett27.spatialflink.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.io.CsvOutputFormat;
import org.apache.flink.api.java.operators.DataSource;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.Path;

import junit.framework.TestCase;

public class TestMapStringTools extends TestCase {

	
	/**
	 * 
	 * @throws Exception
	 */
	public void testConvertToString() throws Exception {
		Map<String, String> t = new HashMap<>();
		t.put("hello", "hello");

		System.out.println(MapStringTools.convertToString(t));

	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testConvertStringWithComma() throws Exception {
		
		Map<String, String> t = new HashMap<>();
		t.put("value", "toto,titi");
		t.put("value2", "toto\"titi");
		
		System.out.println(MapStringTools.convertToString(t));
		
		
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testWriteValuesWithComma() throws Exception {
		
		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
		ArrayList<Tuple2<String,String>> al = new ArrayList<>();
		al.add(new Tuple2<String, String>("hello","1"));
		al.add(new Tuple2<String, String>("toto,tiri,\"","1"));
		DataSource<Tuple2<String, String>> d = env.fromCollection(al);
		
		CsvOutputFormat f = new CsvOutputFormat(new Path("testcsv.csv"));
		f.setQuoteStrings(true);
		
		d.write(f, "testcsv.csv");
		env.execute();
		
	}
	
	/**
	 * 
	 * @throws Exception
	 */
	public void testReadValuesWithCommaAndQuote() throws Exception {
		ExecutionEnvironment env = ExecutionEnvironment.getExecutionEnvironment();
		DataSource<Tuple2<String, String>> r = env.readCsvFile("testcsv.csv").types(String.class, String.class);
		r.print();
	}
	
	

}
