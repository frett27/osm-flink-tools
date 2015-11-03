package org.frett27.spatialflink.model;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.Point;

public class NodeEntity extends SpatialEntity {

	public double x;
	public double y;

	@Override
	public Geometry constructGeometry() {
		Point p = new Point(x, y);
		return p;
	}


}
