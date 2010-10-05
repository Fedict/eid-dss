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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;

import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.applet.service.spi.SignatureService;

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
	 * @param context
	 * @param contentType
	 *            the content-type that this document service should handle.
	 * @throws Exception
	 */
	void init(DSSDocumentContext context, String contentType) throws Exception;

	/**
	 * Checks the incoming document.
	 * 
	 * @param document
	 * @throws Exception
	 */
	void checkIncomingDocument(byte[] document) throws Exception;

	/**
	 * Handles the visualization of the given document.
	 * 
	 * @param document
	 * @param language
	 *            the optional language to be used for visualization.
	 * @return
	 * @throws Exception
	 */
	DocumentVisualization visualizeDocument(byte[] document, String language)
			throws Exception;

	/**
	 * Factory for the signature service that will be used to signed the
	 * document.
	 * 
	 * @param role
	 * 
	 * @return
	 * @throws Exception
	 */
	SignatureService getSignatureService(InputStream documentInputStream,
			TimeStampService timeStampService,
			TimeStampServiceValidator timeStampServiceValidator,
			RevocationDataService revocationDataService,
			SignatureFacet signatureFacet, OutputStream documentOutputStream,
			String role) throws Exception;

	/**
	 * Verifies the signatures on the given document.
	 * 
	 * @param document
	 * @return
	 * @throws Exception
	 */
	List<SignatureInfo> verifySignatures(byte[] document) throws Exception;
}
