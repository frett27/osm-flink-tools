package org.frett27.spatialflink.inputs.parser;

import org.frett27.spatialflink.inputs.OSMContext;
import org.frett27.spatialflink.model.AttributedEntity;

import crosby.binary.Osmformat;
import crosby.binary.Osmformat.PrimitiveBlock;

/**
 * abstract class for parsing the entities
 * 
 * @author use
 *
 * @param <T>
 */
public abstract class Parser<T extends AttributedEntity> {

	/**
	 * retrieve the next entity
	 * 
	 * @return
	 * @throws Exception
	 */
	public abstract T next() throws Exception;

	protected OSMContext createOSMContext(PrimitiveBlock block) {
		assert block != null;
		Osmformat.StringTable stablemessage = block.getStringtable();
		String[] strings = new String[stablemessage.getSCount()];

		for (int i = 0; i < strings.length; i++) {
			strings[i] = stablemessage.getS(i).toStringUtf8();
		}

		int granularity = block.getGranularity();
		long lat_offset = block.getLatOffset();
		long lon_offset = block.getLonOffset();
		int date_granularity = block.getDateGranularity();

		return new OSMContext(granularity, lat_offset, lon_offset, date_granularity, strings);
	}
}
