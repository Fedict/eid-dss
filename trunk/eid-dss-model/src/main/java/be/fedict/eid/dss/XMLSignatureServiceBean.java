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

package be.fedict.eid.dss;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Key;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.servlet.http.HttpSession;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dom.DOMCryptoContext;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.XMLSignContext;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jcp.xml.dsig.internal.dom.DOMKeyInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import be.fedict.eid.applet.service.signer.AbstractXmlSignatureService;
import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;
import be.fedict.eid.applet.service.signer.TemporaryDataStorage;
import be.fedict.eid.applet.service.spi.SignatureService;

@Stateless
@Local(SignatureService.class)
@LocalBinding(jndiBinding = "fedict/eid/dss/XMLSignatureServiceBean")
public class XMLSignatureServiceBean extends AbstractXmlSignatureService {

	private static final Log LOG = LogFactory
			.getLog(XMLSignatureServiceBean.class);

	private TemporaryDataStorage temporaryDataStorage;

	public XMLSignatureServiceBean() {
		this.temporaryDataStorage = new HttpSessionTemporaryDataStorage();
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
			String signedDocument = new String(data);
			HttpSession httpSession = HttpSessionTemporaryDataStorage
					.getHttpSession();
			DocumentRepository documentRepository = new DocumentRepository(
					httpSession);
			documentRepository.setSignedDocument(signedDocument);
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
	protected List<ReferenceInfo> getReferences() {
		List<ReferenceInfo> references = new LinkedList<ReferenceInfo>();
		references.add(new ReferenceInfo("", CanonicalizationMethod.ENVELOPED));
		return references;
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
		String documentStr = documentRepository.getDocument();
		Document document = loadDocument(new ByteArrayInputStream(documentStr
				.getBytes()));
		return document;
	}

	@Override
	protected void postSign(Element signatureElement,
			List<X509Certificate> signingCertificateChain) {
		// TODO: implement as SignatureAspect
		LOG.debug("postSign: adding ds:KeyInfo");
		/*
		 * Make sure we insert right after the ds:SignatureValue element.
		 */
		Node nextSibling;
		NodeList objectNodeList = signatureElement.getElementsByTagNameNS(
				"http://www.w3.org/2000/09/xmldsig#", "Object");
		if (0 == objectNodeList.getLength()) {
			nextSibling = null;
		} else {
			nextSibling = objectNodeList.item(0);
		}
		/*
		 * Add a ds:KeyInfo entry.
		 */
		KeyInfoFactory keyInfoFactory = KeyInfoFactory.getInstance();
		List<Object> x509DataObjects = new LinkedList<Object>();

		/*
		 * Push the signer certificate in the session as it is required for the
		 * response.
		 */
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		X509Certificate signerCertificate = signingCertificateChain.get(0);
		documentRepository.setSignerCertificate(signerCertificate);

		for (X509Certificate certificate : signingCertificateChain) {
			x509DataObjects.add(certificate);
		}
		X509Data x509Data = keyInfoFactory.newX509Data(x509DataObjects);
		List<Object> keyInfoContent = new LinkedList<Object>();
		keyInfoContent.add(x509Data);
		KeyInfo keyInfo = keyInfoFactory.newKeyInfo(keyInfoContent);
		DOMKeyInfo domKeyInfo = (DOMKeyInfo) keyInfo;
		Key key = new Key() {
			private static final long serialVersionUID = 1L;

			public String getAlgorithm() {
				return null;
			}

			public byte[] getEncoded() {
				return null;
			}

			public String getFormat() {
				return null;
			}
		};
		XMLSignContext xmlSignContext = new DOMSignContext(key,
				signatureElement);
		DOMCryptoContext domCryptoContext = (DOMCryptoContext) xmlSignContext;
		String dsPrefix = null;
		try {
			domKeyInfo.marshal(signatureElement, nextSibling, dsPrefix,
					domCryptoContext);
		} catch (MarshalException e) {
			throw new RuntimeException("marshall error: " + e.getMessage(), e);
		}
	}
}
