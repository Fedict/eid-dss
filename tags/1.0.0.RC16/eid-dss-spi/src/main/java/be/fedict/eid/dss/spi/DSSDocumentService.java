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

import be.fedict.eid.applet.service.signer.DigestAlgo;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.applet.service.spi.IdentityDTO;
import be.fedict.eid.applet.service.spi.SignatureServiceEx;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;

/**
 * Document Service interface. A document service interface knows all about a
 * document format, and how to sign it.
 *
 * @author Frank Cornelis
 */
public interface DSSDocumentService extends Serializable {

	/**
	 * Initializes this component.
	 *
	 * @param context
	 *            DSS Document Context
	 * @param contentType
	 *            the content-type that this document service should handle.
	 * @throws Exception
	 *             something went wrong
	 */
	void init(DSSDocumentContext context, String contentType) throws Exception;

	/**
	 * Checks the incoming document.
	 *
	 * @param document
	 *            document to check
	 * @throws Exception
	 *             something went wrong
	 */
	void checkIncomingDocument(byte[] document) throws Exception;

	/**
	 * Handles the visualization of the given document.
	 *
	 * @param document
	 *            document to visualize
	 * @param language
	 *            the optional language to be used for visualization.
	 * @return info on how to handle the visualization
	 * @throws Exception
	 *             something went wrong
	 */
	DocumentVisualization visualizeDocument(byte[] document, String language,
                                            List<MimeType> mimeTypes, String documentViewerServlet)
			throws Exception;

    DocumentVisualization findDocument(byte[] parentDocument, String resourceId)
            throws Exception;

	/**
	 * Factory for the signature service that will be used to signed the
	 * document. A new instance is being created for both preSign and postSign
	 * phases.
	 *
	 * @param documentInputStream
	 *            input stream to the to-be-signed document
	 * @param timeStampService
	 *            timestamping service
	 * @param timeStampServiceValidator
	 *            timestamp service validator
	 * @param revocationDataService
	 *            revocation service
	 * @param signatureFacet
	 *            signature facet to be applied
	 * @param documentOutputStream
	 *            output stream for the signed document
	 * @param role
	 *            optional role
	 * @param identity
	 *            optional identity data object
	 * @param photo
	 *            photo
	 * @param signatureDigestAlgo
	 *            optional signature digest algorithm
	 * @return the signature service implementation to use
	 * @throws Exception
	 *             something went wrong
	 */
	SignatureServiceEx getSignatureService(InputStream documentInputStream,
			TimeStampService timeStampService,
			TimeStampServiceValidator timeStampServiceValidator,
			RevocationDataService revocationDataService,
			SignatureFacet signatureFacet, OutputStream documentOutputStream,
			String role, IdentityDTO identity, byte[] photo,
			DigestAlgo signatureDigestAlgo) throws Exception;

	/**
	 * Verifies the signatures on the given document.
	 *
	 * @param document
	 *            document to be verified.
	 * @param originalDocument
	 *            the optional original document.
	 * @return list of signature info data objects
	 * @throws Exception
	 *             something went wrong
	 */
	List<SignatureInfo> verifySignatures(byte[] document,
			byte[] originalDocument) throws Exception;
}
