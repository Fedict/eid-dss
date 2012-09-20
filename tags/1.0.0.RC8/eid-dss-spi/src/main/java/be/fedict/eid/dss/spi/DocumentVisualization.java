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

package be.fedict.eid.dss.spi;

import java.io.Serializable;

/**
 * Holds information on how to visualize a document.
 * 
 * @author Frank Cornelis
 * 
 */
public class DocumentVisualization implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String browserContentType;

	private final byte[] browserData;

	/**
	 * Main constructor.
	 * 
	 * @param browserContentType
	 *            the content-type that will be send to the web browser.
	 * @param browserData
	 *            the data that will be send to the web browser.
	 */
	public DocumentVisualization(String browserContentType, byte[] browserData) {
		this.browserContentType = browserContentType;
		this.browserData = browserData;
	}

	public String getBrowserContentType() {
		return this.browserContentType;
	}

	public byte[] getBrowserData() {
		return this.browserData;
	}
}
