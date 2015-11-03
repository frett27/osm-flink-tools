package org.frett27.spatialflink.model;

import org.apache.flink.types.ByteValue;

import com.esri.core.geometry.Geometry;
import com.esri.core.geometry.GeometryEngine;

public class ComplexEntity extends SpatialEntity {

	public Geometry.Type geomType;
	public byte[] shapeGeometry;

	@Override
	public Geometry constructGeometry() {
		return GeometryEngine.geometryFromEsriShape(shapeGeometry, geomType);
	}

}
