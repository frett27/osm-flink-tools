package org.frett27.spatialflink.inputs;

import org.frett27.spatialflink.inputs.parser.Parser;
import org.frett27.spatialflink.inputs.parser.WayParser;
import org.frett27.spatialflink.model.WayEntity;

import crosby.binary.Osmformat.PrimitiveBlock;

public class OSMPBFWayInputFormat extends OSMPBFInputFormat<WayEntity> {

	@Override
	protected Parser createParser(PrimitiveBlock p) {
		return new WayParser(p);
	}

}
