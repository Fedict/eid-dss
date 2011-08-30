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

import be.fedict.eid.applet.service.signer.*;
import be.fedict.eid.applet.service.signer.facets.*;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.applet.service.spi.AddressDTO;
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.applet.service.spi.IdentityDTO;
import be.fedict.eid.applet.service.spi.SignatureServiceEx;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * XML signature service. Will create XAdES-X-L v1.4.1 co-signatures.
 * 
 * @author Frank Cornelis
 */
public class XMLSignatureService extends AbstractXmlSignatureService implements
		SignatureServiceEx {

	private final TemporaryDataStorage temporaryDataStorage;

	private final InputStream documentInputStream;

	private final OutputStream documentOutputStream;

	public XMLSignatureService(TimeStampServiceValidator validator,
			RevocationDataService revocationDataService,
			SignatureFacet signatureFacet, InputStream documentInputStream,
			OutputStream documentOutputStream,
			TimeStampService timeStampService, String role,
			IdentityDTO identity, byte[] photo, DigestAlgo signatureDigestAlgo) {

		super(signatureDigestAlgo);
		this.temporaryDataStorage = new HttpSessionTemporaryDataStorage();
		this.documentInputStream = documentInputStream;
		this.documentOutputStream = documentOutputStream;

		addSignatureFacet(new CoSignatureFacet(getSignatureDigestAlgorithm()));
		addSignatureFacet(new KeyInfoSignatureFacet(true, false, false));
		XAdESSignatureFacet xadesSignatureFacet = new XAdESSignatureFacet(
				getSignatureDigestAlgorithm());
		xadesSignatureFacet.setRole(role);
		addSignatureFacet(xadesSignatureFacet);
		addSignatureFacet(new XAdESXLSignatureFacet(timeStampService,
				revocationDataService, getSignatureDigestAlgorithm()));
		addSignatureFacet(signatureFacet);

		setSignatureNamespacePrefix("ds");

		if (null != identity) {
			IdentitySignatureFacet identitySignatureFacet = new IdentitySignatureFacet(
					identity, photo, getSignatureDigestAlgorithm());
			addSignatureFacet(identitySignatureFacet);
		}
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
	protected Document getEnvelopingDocument()
			throws ParserConfigurationException, IOException, SAXException {
		Document document = loadDocument(this.documentInputStream);
		return document;
	}

	public DigestInfo preSign(List<DigestInfo> digestInfos,
			List<X509Certificate> signingCertificateChain,
			IdentityDTO identity, AddressDTO address, byte[] photo)
			throws NoSuchAlgorithmException {
		return super.preSign(digestInfos, signingCertificateChain);
	}
}
