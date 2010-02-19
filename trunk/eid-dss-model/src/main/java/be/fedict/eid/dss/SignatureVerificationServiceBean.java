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

package be.fedict.eid.dss;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import be.fedict.eid.applet.service.signer.KeyInfoKeySelector;

@Stateless
public class SignatureVerificationServiceBean implements
		SignatureVerificationService {

	private static final Log LOG = LogFactory
			.getLog(SignatureVerificationServiceBean.class);

	private DocumentBuilder builder;

	@PostConstruct
	public void postConstruct() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		try {
			this.builder = factory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(
					"parser contig error: " + e.getMessage(), e);
		}
	}

	public List<X509Certificate> verify(byte[] xmlData)
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
		List<X509Certificate> signatories = new LinkedList<X509Certificate>();
		NodeList signatureNodeList = document.getElementsByTagNameNS(
				XMLSignature.XMLNS, "Signature");
		if (0 == signatureNodeList.getLength()) {
			LOG.debug("no XML signature found");
			return signatories;
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
			if (signatureValidity) {
				X509Certificate certificate = keyInfoKeySelector
						.getCertificate();
				// TODO: validate via eID Trust Service
				signatories.add(certificate);
			} else {
				LOG.error("invalid signature");
				throw new InvalidSignatureException();
			}
		}
		return signatories;
	}
}
