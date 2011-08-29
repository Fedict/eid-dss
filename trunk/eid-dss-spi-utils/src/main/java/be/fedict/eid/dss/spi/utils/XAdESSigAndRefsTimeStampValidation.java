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

package be.fedict.eid.dss.spi.utils;

import java.util.List;

import javax.xml.crypto.dsig.XMLSignature;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.tsp.TimeStampToken;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import be.fedict.eid.applet.service.signer.jaxb.xades132.XAdESTimeStampType;
import be.fedict.eid.dss.spi.utils.exception.XAdESValidationException;

/**
 * XAdES SigAndRefsTimeStamp validator. Used by
 * {@link be.fedict.eid.dss.spi.utils.XAdESValidation}.
 * 
 * @author Wim Vandenhaute
 */
public abstract class XAdESSigAndRefsTimeStampValidation {

	private static final Log LOG = LogFactory
			.getLog(XAdESSigAndRefsTimeStampValidation.class);

	public static List<TimeStampToken> verify(
			XAdESTimeStampType sigAndRefsTimeStamp, Element signatureElement)
			throws XAdESValidationException {

		LOG.debug("validate SigAndRefsTimeStamp...");

		List<TimeStampToken> timeStampTokens = XAdESUtils
				.getTimeStampTokens(sigAndRefsTimeStamp);
		if (timeStampTokens.isEmpty()) {
			LOG.error("No timestamp tokens present in SigAndRefsTimeStamp");
			throw new XAdESValidationException(
					"No timestamp tokens present in SigAndRefsTimeStamp");
		}

		TimeStampDigestInput digestInput = new TimeStampDigestInput(
				sigAndRefsTimeStamp.getCanonicalizationMethod().getAlgorithm());

		/*
		 * 2. check ds:SignatureValue present 3. take ds:SignatureValue,
		 * cannonicalize and concatenate bytes.
		 */
		NodeList signatureValueNodeList = signatureElement
				.getElementsByTagNameNS(XMLSignature.XMLNS, "SignatureValue");
		if (0 == signatureValueNodeList.getLength()) {
			LOG.error("no XML signature valuefound");
			throw new XAdESValidationException("no XML signature valuefound");
		}
		digestInput.addNode(signatureValueNodeList.item(0));

		/*
		 * 4. check SignatureTimeStamp(s), CompleteCertificateRefs,
		 * CompleteRevocationRefs, AttributeCertificateRefs,
		 * AttributeRevocationRefs 5. canonicalize these and concatenate to
		 * bytestream from step 3 These nodes should be added in their order of
		 * appearance.
		 */

		NodeList unsignedSignaturePropertiesNodeList = signatureElement
				.getElementsByTagNameNS(XAdESUtils.XADES_132_NS_URI,
						"UnsignedSignatureProperties");
		if (unsignedSignaturePropertiesNodeList.getLength() == 0) {
			throw new XAdESValidationException(
					"UnsignedSignatureProperties node not present");
		}
		Node unsignedSignaturePropertiesNode = unsignedSignaturePropertiesNodeList
				.item(0);
		NodeList childNodes = unsignedSignaturePropertiesNode.getChildNodes();
		int childNodesCount = childNodes.getLength();
		for (int idx = 0; idx < childNodesCount; idx++) {
			Node childNode = childNodes.item(idx);
			if (Node.ELEMENT_NODE != childNode.getNodeType()) {
				continue;
			}
			if (!XAdESUtils.XADES_132_NS_URI
					.equals(childNode.getNamespaceURI())) {
				continue;
			}
			String localName = childNode.getLocalName();
			if ("SignatureTimeStamp".equals(localName)) {
				digestInput.addNode(childNode);
				continue;
			}
			if ("CompleteCertificateRefs".equals(localName)) {
				digestInput.addNode(childNode);
				continue;
			}
			if ("CompleteRevocationRefs".equals(localName)) {
				digestInput.addNode(childNode);
				continue;
			}
			if ("AttributeCertificateRefs".equals(localName)) {
				digestInput.addNode(childNode);
				continue;
			}
			if ("AttributeRevocationRefs".equals(localName)) {
				digestInput.addNode(childNode);
				continue;
			}
		}

		for (TimeStampToken timeStampToken : timeStampTokens) {

			// 1. verify signature in timestamp token
			XAdESUtils.verifyTimeStampTokenSignature(timeStampToken);

			// 6. compute digest and compare with token
			XAdESUtils.verifyTimeStampTokenDigest(timeStampToken, digestInput);
		}

		return timeStampTokens;
	}
}
