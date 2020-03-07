package org.frett27.polygonconstruct;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.frett27.spatialflink.tools.IInvalidPolygonConstructionFeedBack;
import org.frett27.spatialflink.tools.MultiPathAndRole;
import org.frett27.spatialflink.tools.PolygonCreator;
import org.frett27.spatialflink.tools.Role;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.Test;

import com.esri.core.geometry.GeometryEngine;
import com.esri.core.geometry.MapGeometry;
import com.esri.core.geometry.MultiPath;
import com.esri.core.geometry.Polygon;

public class TestIsolateBadPolygons {
 
	private static final String FAIL_TO_CONSTRUCT_POLY = "fail to construct poly :";
	private static final File storefolder = new File("./src/test/resources/org/frett27/spatialflink/jsonfiles");

	@Test
	public void explodePolyErrors() throws Exception {

		BufferedReader r = new BufferedReader(new FileReader(new File("france-latest_20161010.log")));
		try {
			String s = null;
			int cpt = 0;
			while ((s = r.readLine()) != null) {
				if (s.startsWith(FAIL_TO_CONSTRUCT_POLY)) {
					cpt++;
					String json = s.substring(FAIL_TO_CONSTRUCT_POLY.length());

					String id = r.readLine();
					id = id.substring("Error creating polygon ".length());
					long i = -1;
					try {
						i = Long.parseLong(id);
					} catch (NumberFormatException ex) {
						i = -99999 + cpt;
						continue;
					}
					JSONObject jo = new JSONObject(new JSONTokener(json));

					File f = new File(storefolder, "test_file_" + i);
					FileOutputStream fos = new FileOutputStream(f);
					try {
						fos.write(jo.toString(5).getBytes());
					} finally {
						fos.close();
					}
					System.out.println("" + cpt + " handled");
				}
			}
		} finally {
			r.close();
		}
	}

	private int MAX = 187092;

	public ArrayList<MultiPathAndRole> loadElements(int i) throws Exception {

		File f = new File(storefolder, "test_file_" + i);
		FileReader fr = new FileReader(f);
		try {
			JSONObject jo = new JSONObject(new JSONTokener(fr));
			JSONArray origin = jo.getJSONArray("origin");
			ArrayList<MultiPathAndRole> m = new ArrayList<MultiPathAndRole>(origin.length());
			for (int j = 0; j < origin.length(); j++) {

				JSONObject r = origin.getJSONObject(j);

				MapGeometry geometry = GeometryEngine.jsonToGeometry(r.getJSONObject("geometry").toString());

				m.add(new MultiPathAndRole((MultiPath) geometry.getGeometry(), Role.valueOf(r.getString("role"))));

			}

			return m;

		} finally {
			fr.close();
		}

	}

	@Test
	public void test2() throws Exception {
		ArrayList<MultiPathAndRole> a = loadElements(6631260);
		PolygonCreator.createPolygon(a);
	}

	@Test
	public void test1() throws Exception {

		int cpt = 0;
		for (int i = 1; i < MAX; i++) {
			ArrayList<MultiPathAndRole> a = loadElements(i);
			int size = a.size();
			// System.out.println("evaluate " + i + " nb elements :" +
			// ret.length);
			try {
				Polygon polygon = PolygonCreator.createPolygon(a, new IInvalidPolygonConstructionFeedBack() {

					@Override
					public void polygonCreationFeedBackReport(List<MultiPathAndRole> elements, String reason)
							throws Exception {

					}
				});
				System.out.println(
						"" + i + " OK -> " + (cpt++) + " nb parts :" + size + " nbpoints :" + polygon.getPointCount());
			} catch (Exception e) {

			}
		}
	}

}
