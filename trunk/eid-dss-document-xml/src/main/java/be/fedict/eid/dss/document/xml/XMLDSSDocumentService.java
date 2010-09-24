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
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.datatype.XMLGregorianCalendar;
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

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;
import org.bouncycastle.ocsp.OCSPResp;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import be.fedict.eid.applet.service.signer.KeyInfoKeySelector;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CRLValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.EncapsulatedPKIDataType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.OCSPValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.ObjectFactory;
import be.fedict.eid.applet.service.signer.jaxb.xades132.QualifyingPropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.RevocationValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.SignedPropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.SignedSignaturePropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.UnsignedPropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.UnsignedSignaturePropertiesType;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.applet.service.spi.SignatureService;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.DSSDocumentService;
import be.fedict.eid.dss.spi.DocumentVisualization;
import be.fedict.eid.dss.spi.SignatureInfo;

/**
 * Document Service implementation for XML documents.
 * 
 * @author Frank Cornelis
 * 
 */
public class XMLDSSDocumentService implements DSSDocumentService {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(XMLDSSDocumentService.class);

	private DocumentBuilder documentBuilder;

	private DSSDocumentContext context;

	private TransformerFactory transformerFactory;

	private Unmarshaller unmarshaller;

	private CertificateFactory certificateFactory;

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

		JAXBContext jaxbContext = JAXBContext
				.newInstance(
						ObjectFactory.class,
						be.fedict.eid.applet.service.signer.jaxb.xades141.ObjectFactory.class);
		this.unmarshaller = jaxbContext.createUnmarshaller();

		this.certificateFactory = CertificateFactory.getInstance("X.509");
	}

	public SignatureService getSignatureService(
			InputStream documentInputStream, TimeStampService timeStampService,
			TimeStampServiceValidator timeStampServiceValidator,
			RevocationDataService revocationDataService,
			SignatureFacet signatureFacet, OutputStream documentOutputStream) {
		return new XMLSignatureService(timeStampServiceValidator,
				revocationDataService, signatureFacet, documentInputStream,
				documentOutputStream, timeStampService);
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

		DocumentVisualization documentVisualization = new DocumentVisualization(
				browserContentType, browserData);
		return documentVisualization;
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
		for (int signatureNodeIdx = 0; signatureNodeIdx < signatureNodeList
				.getLength(); signatureNodeIdx++) {
			/*
			 * Check signature.
			 */
			Node signatureNode = signatureNodeList.item(signatureNodeIdx);
			KeyInfoKeySelector keyInfoKeySelector = new KeyInfoKeySelector();
			DOMValidateContext domValidateContext = new DOMValidateContext(
					keyInfoKeySelector, signatureNode);
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
			boolean signatureValidity = xmlSignature
					.validate(domValidateContext);
			LOG.debug("signature valid: " + signatureValidity);
			if (false == signatureValidity) {
				LOG.error("invalid signature");
				throw new RuntimeException("invalid signature");
			}

			/*
			 * Get signing time from XAdES-BES extension.
			 */
			SignedInfo signedInfo = xmlSignature.getSignedInfo();
			List<Reference> references = signedInfo.getReferences();
			String xadesSignedPropertiesUri = null;
			for (Reference reference : references) {
				if ("http://uri.etsi.org/01903#SignedProperties"
						.equals(reference.getType())) {
					xadesSignedPropertiesUri = reference.getURI();
					break;
				}
			}
			if (null == xadesSignedPropertiesUri) {
				LOG.error("no XAdES SignedProperties as part of signed XML data");
				throw new RuntimeException("no XAdES SignedProperties");
			}
			String xadesSignedPropertiesId = xadesSignedPropertiesUri
					.substring(1);
			Element nsElement = document.createElement("nsElement");
			nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
					Constants.SignatureSpecNS);
			nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:xades",
					"http://uri.etsi.org/01903/v1.3.2#");
			Node xadesQualifyingPropertiesNode = XPathAPI.selectSingleNode(
					signatureNode,
					"ds:Object/xades:QualifyingProperties[xades:SignedProperties/@Id='"
							+ xadesSignedPropertiesId + "']", nsElement);

			JAXBElement<QualifyingPropertiesType> qualifyingPropertiesElement = (JAXBElement<QualifyingPropertiesType>) this.unmarshaller
					.unmarshal(xadesQualifyingPropertiesNode);
			QualifyingPropertiesType qualifyingProperties = qualifyingPropertiesElement
					.getValue();
			SignedPropertiesType signedProperties = qualifyingProperties
					.getSignedProperties();
			SignedSignaturePropertiesType signedSignatureProperties = signedProperties
					.getSignedSignatureProperties();
			XMLGregorianCalendar signingTimeXMLGregorianCalendar = signedSignatureProperties
					.getSigningTime();
			Date signingTime = signingTimeXMLGregorianCalendar
					.toGregorianCalendar().getTime();
			LOG.debug("XAdES signing time: " + signingTime);
			// TODO: validate XAdES signing certificate
			// TODO: validate XAdES timestamps

			/*
			 * Retrieve certificate chain for trust validation
			 */
			KeyInfo keyInfo = xmlSignature.getKeyInfo();
			List<Object> keyInfoContentList = keyInfo.getContent();
			List<X509Certificate> certificateChain = new LinkedList<X509Certificate>();
			for (Object keyInfoContent : keyInfoContentList) {
				LOG.debug("key info content type: "
						+ keyInfoContent.getClass().getName());
				if (keyInfoContent instanceof X509Data) {
					X509Data x509Data = (X509Data) keyInfoContent;
					List<Object> x509DataContentList = x509Data.getContent();
					for (Object x509DataContent : x509DataContentList) {
						LOG.debug("x509 data content type: "
								+ x509DataContent.getClass().getName());
						if (x509DataContent instanceof X509Certificate) {
							X509Certificate certificate = (X509Certificate) x509DataContent;
							certificateChain.add(certificate);
						}
					}
				}
			}
			if (certificateChain.isEmpty()) {
				LOG.error("no certificate chain present in ds:KeyInfo");
				throw new RuntimeException("no cert chain in ds:KeyInfo");
			}

			/*
			 * Retrieve revocation data from XAdES-X-L extension for trust
			 * validation.
			 */
			List<X509CRL> crls = new LinkedList<X509CRL>();
			List<OCSPResp> ocspResponses = new LinkedList<OCSPResp>();
			UnsignedPropertiesType unsignedProperties = qualifyingProperties
					.getUnsignedProperties();
			UnsignedSignaturePropertiesType unsignedSignatureProperties = unsignedProperties
					.getUnsignedSignatureProperties();
			List<Object> unsignedSignaturePropertiesContentList = unsignedSignatureProperties
					.getCounterSignatureOrSignatureTimeStampOrCompleteCertificateRefs();
			for (Object unsignedSignatureProperty : unsignedSignaturePropertiesContentList) {
				if (false == unsignedSignatureProperty instanceof JAXBElement) {
					continue;
				}
				JAXBElement<?> unsignedSignaturePropertyElement = (JAXBElement<?>) unsignedSignatureProperty;
				Object unsignedSignaturePropertyValue = unsignedSignaturePropertyElement
						.getValue();
				if (unsignedSignaturePropertyValue instanceof RevocationValuesType) {
					RevocationValuesType revocationValues = (RevocationValuesType) unsignedSignaturePropertyValue;
					CRLValuesType crlValues = revocationValues.getCRLValues();
					List<EncapsulatedPKIDataType> crlValuesList = crlValues
							.getEncapsulatedCRLValue();
					for (EncapsulatedPKIDataType crlValue : crlValuesList) {
						byte[] encodedCrl = crlValue.getValue();
						X509CRL crl = (X509CRL) this.certificateFactory
								.generateCRL(new ByteArrayInputStream(
										encodedCrl));
						crls.add(crl);
					}
					OCSPValuesType ocspValues = revocationValues
							.getOCSPValues();
					List<EncapsulatedPKIDataType> ocspValuesList = ocspValues
							.getEncapsulatedOCSPValue();
					for (EncapsulatedPKIDataType ocspValue : ocspValuesList) {
						byte[] encodedOcspResponse = ocspValue.getValue();
						OCSPResp ocspResp = new OCSPResp(encodedOcspResponse);
						ocspResponses.add(ocspResp);
					}
				}
			}

			/*
			 * Perform trust validation via eID Trust Service
			 */
			this.context.validate(certificateChain, signingTime, ocspResponses,
					crls);

			X509Certificate certificate = keyInfoKeySelector.getCertificate();
			SignatureInfo signatureInfo = new SignatureInfo(certificate,
					signingTime);
			signatureInfos.add(signatureInfo);
		}
		return signatureInfos;
	}
}
