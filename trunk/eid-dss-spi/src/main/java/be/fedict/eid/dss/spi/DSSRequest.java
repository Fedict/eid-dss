/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009-2010 FedICT.
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

package be.fedict.eid.dss.spi;

import java.io.Serializable;

/**
 * Represents a DSS request after unmarshalling by the DSSProtocolService.
 * 
 * @author Frank Cornelis
 * 
 */
public class DSSRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	private final byte[] documentData;

	private final String contentType;

	/**
	 * Main constructor.
	 * 
	 * @param documentData
	 * @param contentType
	 */
	public DSSRequest(byte[] documentData, String contentType) {
		this.documentData = documentData;
		this.contentType = contentType;
	}

	/**
	 * The data bytes of the document to be signed.
	 * 
	 * @return
	 */
	public byte[] getDocumentData() {
		return this.documentData;
	}

	/**
	 * The content type of the document to be signed.
	 * 
	 * @return
	 */
	public String getContentType() {
		return this.contentType;
	}
}
