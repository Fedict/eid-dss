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

package be.fedict.eid.dss.document.xml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;

import be.fedict.eid.dss.spi.DSSDocumentContext;

public class SignatureServiceLSResourceResolver implements LSResourceResolver {

	private static final Log LOG = LogFactory
			.getLog(SignatureServiceLSResourceResolver.class);

	private final DSSDocumentContext context;

	public SignatureServiceLSResourceResolver(DSSDocumentContext context) {
		this.context = context;
	}

	public LSInput resolveResource(String type, String namespaceURI,
			String publicId, String systemId, String baseURI) {
		LOG.debug("resolve resource");
		LOG.debug("type: " + type);
		LOG.debug("namespace URI: " + namespaceURI);
		LOG.debug("public Id: " + publicId);
		LOG.debug("system Id: " + systemId);
		LOG.debug("base URI: " + baseURI);
		if (false == "http://www.w3.org/2001/XMLSchema".equals(type)) {
			throw new RuntimeException("unsupported type: " + type);
		}
		byte[] xsd = this.context.getXmlSchema(namespaceURI);
		if (null != xsd) {
			SignatureServiceLSInput lsInput = new SignatureServiceLSInput(xsd,
					publicId, systemId, baseURI);
			return lsInput;
		}
		throw new RuntimeException("unsupported namespace: " + namespaceURI);
		/*
		 * Cannot return null here, else the system starts downloading the file.
		 */
	}
}
