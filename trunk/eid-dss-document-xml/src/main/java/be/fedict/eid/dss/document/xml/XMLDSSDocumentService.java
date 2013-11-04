/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010 FedICT.
 * Copyright (C) 2011 Frank Cornelis.
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
import be.fedict.eid.dss.spi.*;
import be.fedict.eid.dss.spi.utils.XAdESUtils;
import be.fedict.eid.dss.spi.utils.XAdESValidation;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.Init;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.exceptions.Base64DecodingException;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.ReferenceNotInitializedException;
import org.apache.xml.security.signature.XMLSignatureException;
import org.apache.xml.security.transforms.TransformationException;
import org.apache.xml.security.transforms.Transforms;
import org.apache.xml.security.transforms.params.XPath2FilterContainer;
import org.apache.xml.security.utils.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
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
import java.util.Arrays;
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

	static {
		/*
		 * Initialize the Apache XML Security library, else we get an NPE on
		 * Transforms.addTransform.
		 */
		Init.init();
	}

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
				signatureDigestAlgo, this.context);
	}

    public DocumentVisualization findDocument(byte[] parentDocument, String resourceId)
            throws Exception {

        return null;
    }

    public DocumentVisualization visualizeDocument(byte[] document,
			String language, List<MimeType> mimeTypes,
            String documentViewerServlet) throws Exception {

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

	@Override
	public List<SignatureInfo> verifySignatures(byte[] documentData,
			byte[] originalDocument) throws Exception {
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
			xadesValidation.prepareDocument(signatureElement);

			KeyInfoKeySelector keyInfoKeySelector = new KeyInfoKeySelector();
			DOMValidateContext domValidateContext = new DOMValidateContext(
					keyInfoKeySelector, signatureElement);
			XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
					.getInstance("DOM", new org.apache.jcp.xml.dsig.internal.dom.XMLDSigRI());
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

			if (null != originalDocument) {
				Document originalDomDocument = XAdESUtils
						.loadDocument(originalDocument);
				LOG.debug("performing original document verification");
				verifyCoSignatureReference(xmlSignature, originalDomDocument);
				LOG.debug("original document verified");
			} else {
				/*
				 * We can still check whether the co-signature ds:Reference is
				 * indeed doing a co-signature.
				 */
				verifyCoSignatureReference(xmlSignature, document);
			}

			X509Certificate signingCertificate = keyInfoKeySelector
					.getCertificate();
			SignatureInfo signatureInfo = xadesValidation.validate(document,
					xmlSignature, signatureElement, signingCertificate);
			signatureInfos.add(signatureInfo);
		}
		return signatureInfos;
	}

	private void verifyCoSignatureReference(XMLSignature xmlSignature,
			Document originalDomDocument) throws XMLSecurityException,
			TransformationException, XMLSignatureException,
			ReferenceNotInitializedException, Base64DecodingException {
		SignedInfo signedInfo = xmlSignature.getSignedInfo();
		@SuppressWarnings("unchecked")
		List<Reference> references = signedInfo.getReferences();
		for (Reference reference : references) {
			LOG.debug("reference type: " + reference.getType());
			if (null != reference.getType()) {
				/*
				 * We skip XAdES and eID identity ds:Reference.
				 */
				continue;
			}
			String digestAlgo = reference.getDigestMethod().getAlgorithm();
			LOG.debug("ds:Reference digest algo: " + digestAlgo);
			byte[] digestValue = reference.getDigestValue();

			// xmlsec 1.5 changed the constructor
			org.apache.xml.security.signature.XMLSignature xmldsig = new org.apache.xml.security.signature.XMLSignature(
					originalDomDocument, "",
					org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA512,
					Canonicalizer.ALGO_ID_C14N_EXCL_WITH_COMMENTS);

			Transforms transforms = new Transforms(originalDomDocument);

            // XPath v1 - slow
//            XPathContainer xpath = new XPathContainer(originalDomDocument);
//			xpath.setXPathNamespaceContext("ds", Constants.SignatureSpecNS);
//			xpath.setXPath("not(ancestor-or-self::ds:Signature)");
//			transforms.addTransform(Transforms.TRANSFORM_XPATH,
//					xpath.getElementPlusReturns());

            // XPath v2 - fast
            XPath2FilterContainer xpath = XPath2FilterContainer.newInstanceSubtract(originalDomDocument,
                    "/descendant::*[name()='ds:Signature']");
			xpath.setXPathNamespaceContext("ds", Constants.SignatureSpecNS);
			transforms.addTransform(Transforms.TRANSFORM_XPATH2FILTER,
					xpath.getElementPlusReturns());

			transforms
					.addTransform(Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
			xmldsig.addDocument("", transforms, digestAlgo);

			org.apache.xml.security.signature.SignedInfo apacheSignedInfo = xmldsig
					.getSignedInfo();
			org.apache.xml.security.signature.Reference apacheReference = apacheSignedInfo
					.item(0);
			apacheReference.generateDigestValue();
			byte[] originalDigestValue = apacheReference.getDigestValue();
			if (false == Arrays.equals(originalDigestValue, digestValue)) {
				throw new RuntimeException("not original document");
			}
			LOG.debug("co-signature ds:Reference checked");
		}
	}
}
