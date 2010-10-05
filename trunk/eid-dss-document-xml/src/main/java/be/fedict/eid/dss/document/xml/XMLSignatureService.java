/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009 FedICT.
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import be.fedict.eid.applet.service.signer.AbstractXmlSignatureService;
import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.TemporaryDataStorage;
import be.fedict.eid.applet.service.signer.facets.CoSignatureFacet;
import be.fedict.eid.applet.service.signer.facets.KeyInfoSignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.facets.XAdESSignatureFacet;
import be.fedict.eid.applet.service.signer.facets.XAdESXLSignatureFacet;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;

/**
 * XML signature service. Will create XAdES-X-L v1.4.1 co-signatures.
 * 
 * @author Frank Cornelis
 * 
 */
public class XMLSignatureService extends AbstractXmlSignatureService {

	private final TemporaryDataStorage temporaryDataStorage;

	private final InputStream documentInputStream;

	private final OutputStream documentOutputStream;

	public XMLSignatureService(TimeStampServiceValidator validator,
			RevocationDataService revocationDataService,
			SignatureFacet signatureFacet, InputStream documentInputStream,
			OutputStream documentOutputStream,
			TimeStampService timeStampService, String role) {
		this.temporaryDataStorage = new HttpSessionTemporaryDataStorage();
		this.documentInputStream = documentInputStream;
		this.documentOutputStream = documentOutputStream;

		addSignatureFacet(new CoSignatureFacet("SHA-512"));
		addSignatureFacet(new KeyInfoSignatureFacet(true, false, false));
		XAdESSignatureFacet xadesSignatureFacet = new XAdESSignatureFacet(
				"SHA-512");
		xadesSignatureFacet.setRole(role);
		addSignatureFacet(xadesSignatureFacet);
		addSignatureFacet(new XAdESXLSignatureFacet(timeStampService,
				revocationDataService, "SHA-512"));
		addSignatureFacet(signatureFacet);

		setSignatureNamespacePrefix("ds");
	}

	@Override
	protected OutputStream getSignedDocumentOutputStream() {
		return this.documentOutputStream;
	}

	@Override
	protected TemporaryDataStorage getTemporaryDataStorage() {
		return this.temporaryDataStorage;
	}

	public String getFilesDigestAlgorithm() {
		return null;
	}

	@Override
	protected String getSignatureDigestAlgorithm() {
		return "SHA-512";
	}

	@Override
	protected Document getEnvelopingDocument()
			throws ParserConfigurationException, IOException, SAXException {
		Document document = loadDocument(this.documentInputStream);
		return document;
	}
}
