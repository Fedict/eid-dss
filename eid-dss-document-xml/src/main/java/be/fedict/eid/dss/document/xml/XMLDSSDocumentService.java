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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.applet.service.spi.SignatureService;
import be.fedict.eid.dss.spi.DSSDocumentService;

/**
 * Document Service implementation for XML documents.
 * 
 * @author Frank Cornelis
 * 
 */
public class XMLDSSDocumentService implements DSSDocumentService {

	private static final long serialVersionUID = 1L;

	private DocumentBuilder documentBuilder;

	public void checkIncomingDocument(byte[] document) throws Exception {
		ByteArrayInputStream documentInputStream = new ByteArrayInputStream(
				document);
		this.documentBuilder.parse(documentInputStream);
	}

	public void init(ServletContext servletContext) throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
	}

	public SignatureService getSignatureService(
			InputStream documentInputStream,
			TimeStampService timeStampService,
			TimeStampServiceValidator timeStampServiceValidator, RevocationDataService revocationDataService,
			SignatureFacet signatureFacet, OutputStream documentOutputStream) {
		return new XMLSignatureService(timeStampServiceValidator,
				revocationDataService, signatureFacet, documentInputStream,
				documentOutputStream, timeStampService);
	}
}
