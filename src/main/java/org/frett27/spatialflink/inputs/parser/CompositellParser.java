package org.frett27.spatialflink.inputs.parser;

import java.util.ArrayList;

import org.frett27.spatialflink.model.AttributedEntity;

import crosby.binary.Osmformat.PrimitiveBlock;

public class CompositellParser extends Parser<AttributedEntity> {

	private PrimitiveBlock block;

	private ArrayList<Parser> parserList = new ArrayList<>();

	public CompositellParser(PrimitiveBlock block) {
		this.block = block;

		parserList.add(new NodeParser(block));
		parserList.add(new WayParser(block));
		parserList.add(new RelationParser(block));

	}

	@Override
	public AttributedEntity next() throws Exception {

		if (parserList.size() == 0) {
			return null;
		}

		while (parserList.size() > 0) {
			AttributedEntity att = parserList.get(0).next();
			if (att != null) {
				return att;
			}
			parserList.remove(0);
		}

		return null;
	}

}
