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

package be.fedict.eid.dss.webapp;

import be.fedict.eid.dss.model.XmlSchemaManager;
import be.fedict.eid.dss.model.XmlStyleSheetManager;
import be.fedict.eid.dss.spi.DSSDocumentContext;

/**
 * Implementation of DSS document context.
 * 
 * @author Frank Cornelis
 * 
 */
public class DSSDocumentContextImpl implements DSSDocumentContext {

	private static final long serialVersionUID = 1L;

	private final XmlSchemaManager xmlSchemaManager;

	private final XmlStyleSheetManager xmlStyleSheetManager;

	/**
	 * Main constructor.
	 * 
	 * @param xmlSchemaManager
	 */
	public DSSDocumentContextImpl(XmlSchemaManager xmlSchemaManager,
			XmlStyleSheetManager xmlStyleSheetManager) {
		this.xmlSchemaManager = xmlSchemaManager;
		this.xmlStyleSheetManager = xmlStyleSheetManager;
	}

	public byte[] getXmlSchema(String namespace) {
		return this.xmlSchemaManager.getXmlSchema(namespace);
	}

	public byte[] getXmlStyleSheet(String namespace) {
		return this.xmlStyleSheetManager.getXmlStyleSheet(namespace);
	}
}
