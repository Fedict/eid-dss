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

package be.fedict.eid.dss.model.bean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import be.fedict.eid.applet.service.signer.AbstractXmlSignatureService;
import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;
import be.fedict.eid.applet.service.signer.TemporaryDataStorage;
import be.fedict.eid.applet.service.signer.facets.EnvelopedSignatureFacet;
import be.fedict.eid.applet.service.signer.facets.KeyInfoSignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.facets.XAdESSignatureFacet;
import be.fedict.eid.applet.service.signer.facets.XAdESXLSignatureFacet;
import be.fedict.eid.applet.service.signer.time.TSPTimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.dss.model.DocumentRepository;
import be.fedict.eid.dss.model.SignerCertificateSignatureFacet;
import be.fedict.eid.dss.model.TrustServiceRevocationDataService;
import be.fedict.eid.dss.model.TrustServiceTimeStampServiceValidator;
import be.fedict.eid.dss.spi.SignatureStatus;

/**
 * XML signature service.
 * 
 * @author Frank Cornelis
 * 
 */
public class XMLSignatureService extends AbstractXmlSignatureService {

	private TemporaryDataStorage temporaryDataStorage;

	public XMLSignatureService(String tspUrl, String httpProxyHost,
			int httpProxyPort, String xkmsUrl) {
		this.temporaryDataStorage = new HttpSessionTemporaryDataStorage();
		addSignatureFacet(new EnvelopedSignatureFacet("SHA-512"));
		addSignatureFacet(new KeyInfoSignatureFacet(true, false, false));
		addSignatureFacet(new XAdESSignatureFacet("SHA-512"));

		TimeStampServiceValidator validator = new TrustServiceTimeStampServiceValidator(
				xkmsUrl, httpProxyHost, httpProxyPort);

		TSPTimeStampService timeStampService = new TSPTimeStampService(tspUrl,
				validator);
		if (null != httpProxyHost) {
			timeStampService.setProxy(httpProxyHost, httpProxyPort);
		}
		timeStampService.setDigestAlgo("SHA-512");

		RevocationDataService revocationDataService = new TrustServiceRevocationDataService(
				xkmsUrl, httpProxyHost, httpProxyPort);

		addSignatureFacet(new XAdESXLSignatureFacet(timeStampService,
				revocationDataService, "SHA-512"));
		addSignatureFacet(new SignerCertificateSignatureFacet());
		setSignatureNamespacePrefix("ds");
	}

	@Override
	protected OutputStream getSignedDocumentOutputStream() {
		OutputStream signedDocumentOutputStream = new XMLOutputStream();
		return signedDocumentOutputStream;
	}

	private class XMLOutputStream extends ByteArrayOutputStream {
		private final Log LOG = LogFactory.getLog(XMLOutputStream.class);

		@Override
		public void close() throws IOException {
			LOG.debug("closing XML signed document output stream");
			super.close();
			byte[] data = this.toByteArray();
			LOG.debug("size of signed XML document: " + data.length);
			HttpSession httpSession = HttpSessionTemporaryDataStorage
					.getHttpSession();
			DocumentRepository documentRepository = new DocumentRepository(
					httpSession);
			documentRepository.setSignedDocument(data);
			documentRepository.setSignatureStatus(SignatureStatus.OK);
		}
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
		/*
		 * We use the document that was sent to us via the POST request.
		 */
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		byte[] documentData = documentRepository.getDocument();
		Document document = loadDocument(new ByteArrayInputStream(documentData));
		return document;
	}
}
