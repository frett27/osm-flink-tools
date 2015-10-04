package org.frett27.spatialflink.inputs;

import org.frett27.spatialflink.inputs.parser.Parser;
import org.frett27.spatialflink.inputs.parser.WayParser;
import org.frett27.spatialflink.model.Relation;

import crosby.binary.Osmformat.PrimitiveBlock;

public class OSMPBFWayInputFormat extends OSMPBFInputFormat<Relation> {

	@Override
	protected Parser createParser(PrimitiveBlock p) {
		return new WayParser(p);
	}

}
