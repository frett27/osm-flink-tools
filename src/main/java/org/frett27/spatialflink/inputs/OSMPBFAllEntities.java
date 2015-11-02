package org.frett27.spatialflink.inputs;

import org.frett27.spatialflink.inputs.parser.CompositellParser;
import org.frett27.spatialflink.inputs.parser.Parser;
import org.frett27.spatialflink.model.AttributedEntity;

import crosby.binary.Osmformat.PrimitiveBlock;

public class OSMPBFAllEntities extends OSMPBFInputFormat<AttributedEntity> {
	
	@Override
	protected Parser createParser(PrimitiveBlock p) {
		return new CompositellParser(p);
	}

}
