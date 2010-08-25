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

import javax.servlet.ServletContext;

/**
 * Document Service interface. A document service interface knows all about a
 * document format, and how to sign it.
 * 
 * @author Frank Cornelis
 * 
 */
public interface DSSDocumentService extends Serializable {

	/**
	 * Initializes this component.
	 * 
	 * @param servletContext
	 * @throws Exception
	 */
	void init(ServletContext servletContext) throws Exception;

	/**
	 * Checks the incoming document.
	 * 
	 * @param document
	 * @throws Exception
	 */
	void checkIncomingDocument(byte[] document) throws Exception;
}
