package org.frett27.spatialflink.tools;

import java.util.List;

public interface IInvalidPolygonConstructionFeedBack extends IReport {

	/**
	 * report we have a problem constructing a polygon
	 */
	public void polygonCreationFeedBackReport(List<MultiPathAndRole> elements,
			String reason) throws Exception;

}
