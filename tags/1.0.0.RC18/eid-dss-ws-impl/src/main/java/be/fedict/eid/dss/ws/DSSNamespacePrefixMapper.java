/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see 
 * http://www.gnu.org/licenses/.
 */

package be.fedict.eid.dss.ws;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.xml.bind.marshaller.NamespacePrefixMapper;

public class DSSNamespacePrefixMapper extends NamespacePrefixMapper {

	private static final Log LOG = LogFactory
			.getLog(DSSNamespacePrefixMapper.class);

	private static final Map<String, String> prefixes = new HashMap<String, String>();

	static {
		prefixes.put("urn:oasis:names:tc:dss:1.0:core:schema", "dss");
	}

	@Override
	public String getPreferredPrefix(String namespaceUri, String suggestion,
			boolean requirePrefix) {
		LOG.debug("get preferred prefix: " + namespaceUri);
		LOG.debug("suggestion: " + suggestion);
		String prefix = prefixes.get(namespaceUri);
		if (null != prefix) {
			return prefix;
		}
		return suggestion;
	}
}
