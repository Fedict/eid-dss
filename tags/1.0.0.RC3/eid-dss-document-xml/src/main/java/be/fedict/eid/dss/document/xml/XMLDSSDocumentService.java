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

import be.fedict.eid.applet.service.signer.DigestAlgo;
import be.fedict.eid.applet.service.signer.KeyInfoKeySelector;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.applet.service.spi.IdentityDTO;
import be.fedict.eid.applet.service.spi.SignatureServiceEx;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.DSSDocumentService;
import be.fedict.eid.dss.spi.DocumentVisualization;
import be.fedict.eid.dss.spi.SignatureInfo;
import be.fedict.eid.dss.spi.utils.XAdESValidation;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

/**
 * Document Service implementation for XML documents.
 * 
 * @author Frank Cornelis
 */
public class XMLDSSDocumentService implements DSSDocumentService {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(XMLDSSDocumentService.class);

	private DocumentBuilder documentBuilder;

	private DSSDocumentContext context;

	private TransformerFactory transformerFactory;

	public void checkIncomingDocument(byte[] document) throws Exception {

		LOG.debug("checking incoming document");
		ByteArrayInputStream documentInputStream = new ByteArrayInputStream(
				document);
		Document dom = this.documentBuilder.parse(documentInputStream);

		String namespace = dom.getDocumentElement().getNamespaceURI();
		if (null == namespace) {
			LOG.debug("no namespace defined");
			return;
		}

		byte[] xsd = this.context.getXmlSchema(namespace);
		if (null == xsd) {
			LOG.debug("no XML schema available for namespace: " + namespace);
			return;
		}

		LOG.debug("validating against XML schema: " + namespace);
		SchemaFactory schemaFactory = SchemaFactory
				.newInstance("http://www.w3.org/2001/XMLSchema");
		schemaFactory
				.setResourceResolver(new SignatureServiceLSResourceResolver(
						this.context));
		StreamSource schemaSource = new StreamSource(new ByteArrayInputStream(
				xsd));
		Schema schema = schemaFactory.newSchema(schemaSource);
		Validator validator = schema.newValidator();
		DOMSource domSource = new DOMSource(dom);
		validator.validate(domSource);
	}

	public void init(DSSDocumentContext context, String contentType)
			throws Exception {

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
		this.context = context;
		this.transformerFactory = TransformerFactory.newInstance();
	}

	public SignatureServiceEx getSignatureService(
			InputStream documentInputStream, TimeStampService timeStampService,
			TimeStampServiceValidator timeStampServiceValidator,
			RevocationDataService revocationDataService,
			SignatureFacet signatureFacet, OutputStream documentOutputStream,
			String role, IdentityDTO identity, byte[] photo,
			DigestAlgo signatureDigestAlgo) {

		return new XMLSignatureService(timeStampServiceValidator,
				revocationDataService, signatureFacet, documentInputStream,
				documentOutputStream, timeStampService, role, identity, photo,
				signatureDigestAlgo);
	}

	public DocumentVisualization visualizeDocument(byte[] document,
			String language) throws Exception {

		// per default we do nothing
		byte[] browserData = document;
		String browserContentType = "text/xml";

		ByteArrayInputStream documentInputStream = new ByteArrayInputStream(
				document);
		Document dom = this.documentBuilder.parse(documentInputStream);
		String namespace = dom.getDocumentElement().getNamespaceURI();
		if (null != namespace) {
			LOG.debug("document namespace: " + namespace);
			byte[] xsl = this.context.getXmlStyleSheet(namespace);
			if (null != xsl) {
				LOG.debug("XML style sheet present");
				browserContentType = "text/html";
				Transformer transformer = this.transformerFactory
						.newTransformer(new StreamSource(
								new ByteArrayInputStream(xsl)));
				if (null != language) {
					transformer.setParameter("language", language);
				}
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				transformer.transform(new DOMSource(dom), new StreamResult(
						outputStream));
				browserData = outputStream.toByteArray();
			}
		}

		return new DocumentVisualization(browserContentType, browserData);
	}

	public List<SignatureInfo> verifySignatures(byte[] documentData)
			throws Exception {
		Document document = this.documentBuilder
				.parse(new ByteArrayInputStream(documentData));

		List<SignatureInfo> signatureInfos = new LinkedList<SignatureInfo>();
		NodeList signatureNodeList = document.getElementsByTagNameNS(
				XMLSignature.XMLNS, "Signature");
		if (0 == signatureNodeList.getLength()) {
			LOG.debug("no XML signature found");
			return signatureInfos;
		}

		XAdESValidation xadesValidation = new XAdESValidation(this.context);

		for (int signatureNodeIdx = 0; signatureNodeIdx < signatureNodeList
				.getLength(); signatureNodeIdx++) {
			/*
			 * Check signature.
			 */
			Element signatureElement = (Element) signatureNodeList
					.item(signatureNodeIdx);
			KeyInfoKeySelector keyInfoKeySelector = new KeyInfoKeySelector();
			DOMValidateContext domValidateContext = new DOMValidateContext(
					keyInfoKeySelector, signatureElement);
			XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
					.getInstance();
			XMLSignature xmlSignature;
			try {
				xmlSignature = xmlSignatureFactory
						.unmarshalXMLSignature(domValidateContext);
			} catch (MarshalException e) {
				LOG.error("XML signature marshalling error: " + e.getMessage(),
						e);
				continue;
			}
			LOG.debug("validating signature: " + xmlSignature.getId());
			boolean signatureValid = xmlSignature.validate(domValidateContext);
			LOG.debug("signature valid: " + signatureValid);
			if (!signatureValid) {
				LOG.error("invalid signature");
				throw new RuntimeException("invalid signature");
			}

			X509Certificate signingCertificate = keyInfoKeySelector
					.getCertificate();
			SignatureInfo signatureInfo = xadesValidation.validate(document,
					xmlSignature, signatureElement, signingCertificate);
			signatureInfos.add(signatureInfo);
		}
		return signatureInfos;
	}
}
