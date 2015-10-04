package org.frett27.spatialflink.model;

import com.esri.core.geometry.Geometry;

public abstract class SpatialEntity extends AttributedEntity {

	public abstract Geometry constructGeometry();
	
}
