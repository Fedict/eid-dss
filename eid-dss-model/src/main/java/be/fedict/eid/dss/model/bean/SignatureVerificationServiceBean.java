/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009-2010 FedICT.
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
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import be.fedict.eid.applet.service.signer.KeyInfoKeySelector;
import be.fedict.eid.applet.service.signer.jaxb.xades132.ObjectFactory;
import be.fedict.eid.applet.service.signer.jaxb.xades132.QualifyingPropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.SignedPropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.SignedSignaturePropertiesType;
import be.fedict.eid.dss.model.SignatureInfo;
import be.fedict.eid.dss.model.SignatureVerificationService;
import be.fedict.eid.dss.model.exception.DocumentFormatException;
import be.fedict.eid.dss.model.exception.InvalidSignatureException;

@Stateless
public class SignatureVerificationServiceBean implements
		SignatureVerificationService {

	private static final Log LOG = LogFactory
			.getLog(SignatureVerificationServiceBean.class);

	private DocumentBuilder builder;

	private Unmarshaller unmarshaller;

	@PostConstruct
	public void postConstruct() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(true);
		try {
			this.builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(
					"parser contig error: " + e.getMessage(), e);
		}

		try {
			JAXBContext jaxbContext = JAXBContext
					.newInstance(ObjectFactory.class);
			this.unmarshaller = jaxbContext.createUnmarshaller();
		} catch (JAXBException e) {
			throw new RuntimeException("JAXB error: " + e.getMessage(), e);
		}
	}

	public List<SignatureInfo> verify(byte[] xmlData)
			throws DocumentFormatException, InvalidSignatureException {
		String xmlStr = new String(xmlData);
		LOG.debug("verify: " + xmlStr);
		Document document;
		try {
			document = this.builder.parse(new ByteArrayInputStream(xmlData));
		} catch (SAXException e) {
			LOG.error("SAX error: " + e.getMessage(), e);
			throw new DocumentFormatException();
		} catch (IOException e) {
			LOG.error("IO error: " + e.getMessage(), e);
			throw new DocumentFormatException();
		}
		List<SignatureInfo> signatureInfos = new LinkedList<SignatureInfo>();
		NodeList signatureNodeList = document.getElementsByTagNameNS(
				XMLSignature.XMLNS, "Signature");
		if (0 == signatureNodeList.getLength()) {
			LOG.debug("no XML signature found");
			return signatureInfos;
		}
		for (int signatureNodeIdx = 0; signatureNodeIdx < signatureNodeList
				.getLength(); signatureNodeIdx++) {
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
			boolean signatureValidity;
			try {
				signatureValidity = xmlSignature.validate(domValidateContext);
			} catch (XMLSignatureException e) {
				LOG.error("XML signature error: " + e.getMessage(), e);
				throw new InvalidSignatureException();
			}

			if (false == signatureValidity) {
				LOG.error("invalid signature");
				throw new InvalidSignatureException();
			}

			// TODO: signature format validation should go via the document
			// service
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
				throw new InvalidSignatureException();
			}
			String xadesSignedPropertiesId = xadesSignedPropertiesUri
					.substring(1);
			Element nsElement = document.createElement("nsElement");
			nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
					Constants.SignatureSpecNS);
			nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:xades",
					"http://uri.etsi.org/01903/v1.3.2#");
			Node xadesQualifyingPropertiesNode;
			try {
				xadesQualifyingPropertiesNode = XPathAPI.selectSingleNode(
						signatureNode,
						"ds:Object/xades:QualifyingProperties[xades:SignedProperties/@Id='"
								+ xadesSignedPropertiesId + "']", nsElement);
			} catch (TransformerException e) {
				LOG.error("XPATH error: " + e.getMessage(), e);
				throw new InvalidSignatureException();
			}
			JAXBElement<QualifyingPropertiesType> qualifyingPropertiesElement;
			try {
				qualifyingPropertiesElement = (JAXBElement<QualifyingPropertiesType>) this.unmarshaller
						.unmarshal(xadesQualifyingPropertiesNode);
			} catch (JAXBException e) {
				LOG.error("JAXB error: " + e.getMessage(), e);
				throw new InvalidSignatureException();
			}
			QualifyingPropertiesType qualifyingProperties = qualifyingPropertiesElement
					.getValue();
			SignedPropertiesType signedProperties = qualifyingProperties
					.getSignedProperties();
			SignedSignaturePropertiesType signedSignatureProperties = signedProperties
					.getSignedSignatureProperties();
			XMLGregorianCalendar signingTime = signedSignatureProperties
					.getSigningTime();
			// TODO: validate XAdES

			X509Certificate certificate = keyInfoKeySelector.getCertificate();
			// TODO: validate via eID Trust Service
			SignatureInfo signatureInfo = new SignatureInfo(certificate,
					signingTime.toGregorianCalendar().getTime());
			signatureInfos.add(signatureInfo);
		}
		return signatureInfos;
	}
}