package org.frett27.spatialflink.inputs;

import org.frett27.spatialflink.inputs.parser.NodeParser;
import org.frett27.spatialflink.inputs.parser.Parser;
import org.frett27.spatialflink.model.NodeEntity;

import crosby.binary.Osmformat.PrimitiveBlock;

public class OSMPBFNodeInputFormat extends OSMPBFInputFormat<NodeEntity> {

	@Override
	protected Parser createParser(PrimitiveBlock p) {
		return new NodeParser(p);
	}

}
