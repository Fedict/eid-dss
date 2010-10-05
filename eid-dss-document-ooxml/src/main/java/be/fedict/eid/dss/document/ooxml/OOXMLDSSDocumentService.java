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

package be.fedict.eid.dss.document.ooxml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.SignatureProperties;
import javax.xml.crypto.dsig.SignatureProperty;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xpath.XPathAPI;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import be.fedict.eid.applet.service.signer.KeyInfoKeySelector;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.ooxml.OOXMLProvider;
import be.fedict.eid.applet.service.signer.ooxml.OOXMLSignatureVerifier;
import be.fedict.eid.applet.service.signer.ooxml.OOXMLURIDereferencer;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.applet.service.spi.SignatureService;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.DSSDocumentService;
import be.fedict.eid.dss.spi.DocumentVisualization;
import be.fedict.eid.dss.spi.SignatureInfo;

public class OOXMLDSSDocumentService implements DSSDocumentService {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(OOXMLDSSDocumentService.class);

	private DSSDocumentContext documentContext;

	public void init(DSSDocumentContext context, String contentType)
			throws Exception {
		LOG.debug("init");
		this.documentContext = context;
		/*
		 * Next will probably break re-deployments.
		 */
		OOXMLProvider.install();
	}

	public void checkIncomingDocument(byte[] document) throws Exception {
		LOG.debug("checkIncomingDocument");
	}

	public DocumentVisualization visualizeDocument(byte[] document,
			String language) throws Exception {
		LOG.debug("visualizeDocument");
		return null;
	}

	public SignatureService getSignatureService(
			InputStream documentInputStream, TimeStampService timeStampService,
			TimeStampServiceValidator timeStampServiceValidator,
			RevocationDataService revocationDataService,
			SignatureFacet signatureFacet, OutputStream documentOutputStream,
			String role) throws Exception {
		return new OOXMLSignatureService(documentInputStream,
				documentOutputStream, signatureFacet, role);
	}

	public List<SignatureInfo> verifySignatures(byte[] document)
			throws Exception {
		List<String> signatureResourceNames = OOXMLSignatureVerifier
				.getSignatureResourceNames(new ByteArrayInputStream(document));
		List<SignatureInfo> signatureInfos = new LinkedList<SignatureInfo>();
		for (String signatureResourceName : signatureResourceNames) {
			Document signatureDocument = OOXMLSignatureVerifier
					.getSignatureDocument(new ByteArrayInputStream(document),
							signatureResourceName);
			if (null == signatureDocument) {
				continue;
			}
			NodeList signatureNodeList = signatureDocument
					.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
			if (0 == signatureNodeList.getLength()) {
				return null;
			}
			Element signatureElement = (Element) signatureNodeList.item(0);
			KeyInfoKeySelector keySelector = new KeyInfoKeySelector();
			DOMValidateContext domValidateContext = new DOMValidateContext(
					keySelector, signatureElement);
			domValidateContext.setProperty(
					"org.jcp.xml.dsig.validateManifests", Boolean.TRUE);
			OOXMLURIDereferencer dereferencer = new OOXMLURIDereferencer(
					document);
			domValidateContext.setURIDereferencer(dereferencer);

			XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
					.getInstance();
			XMLSignature xmlSignature = xmlSignatureFactory
					.unmarshalXMLSignature(domValidateContext);
			boolean validity = xmlSignature.validate(domValidateContext);
			if (false == validity) {
				LOG.error("signature invalid");
				continue;
			}
			X509Certificate signer = keySelector.getCertificate();
			Date signingTime = null;
			List<XMLObject> xmlObjects = xmlSignature.getObjects();
			for (XMLObject xmlObject : xmlObjects) {
				if (false == "idPackageObject".equals(xmlObject.getId())) {
					continue;
				}
				List<XMLStructure> objectContentList = xmlObject.getContent();
				for (XMLStructure objectContent : objectContentList) {
					if (objectContent instanceof SignatureProperties) {
						LOG.debug("SignatureProperties detected");
						SignatureProperties signatureProperties = (SignatureProperties) objectContent;
						List<SignatureProperty> signaturePropertyList = signatureProperties
								.getProperties();
						for (SignatureProperty signatureProperty : signaturePropertyList) {
							List<XMLStructure> signaturePropertyContentList = signatureProperty
									.getContent();
							for (XMLStructure signaturePropertyContent : signaturePropertyContentList) {
								if (signaturePropertyContent instanceof DOMStructure) {
									DOMStructure signaturePropertyContentDomStructure = (DOMStructure) signaturePropertyContent;
									Node node = signaturePropertyContentDomStructure
											.getNode();
									if (node instanceof Element) {
										Element element = (Element) node;
										LOG.debug("namespace: "
												+ element.getNamespaceURI());
										LOG.debug("local name: "
												+ element.getLocalName());
										if ("http://schemas.openxmlformats.org/package/2006/digital-signature"
												.equals(element
														.getNamespaceURI())
												&& "SignatureTime"
														.equals(element
																.getLocalName())) {
											LOG.debug("SignatureTime detected");
											Node signingTimeValueNode = XPathAPI
													.selectSingleNode(element,
															"mdssi:Value");
											if (null != signingTimeValueNode) {
												LOG.debug("SignatureTime/Value detected");
												String signingTimeStr = signingTimeValueNode
														.getFirstChild()
														.getTextContent();
												LOG.debug("signing time string: "
														+ signingTimeStr);
												DateTimeFormatter parser = ISODateTimeFormat
														.dateTimeParser();
												signingTime = parser
														.parseDateTime(
																signingTimeStr)
														.toDate();
											}
										}
									}
								}
							}
						}
					}
				}
			}
			if (null == signingTime) {
				LOG.error("no siging time present");
				continue;
			}
			SignatureInfo signatureInfo = new SignatureInfo(signer, signingTime);
			signatureInfos.add(signatureInfo);
		}
		return signatureInfos;
	}
}
