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
import org.w3c.dom.NodeList;

import be.fedict.eid.applet.service.signer.jaxb.xades132.XAdESTimeStampType;
import be.fedict.eid.dss.spi.utils.exception.XAdESValidationException;

/**
 * XAdES SignatureTimeStamp validator. Used by {@link XAdESValidation}.
 * 
 * @author Wim Vandenhaute
 */
public abstract class XAdESSignatureTimeStampValidation {

	private static final Log LOG = LogFactory
			.getLog(XAdESSignatureTimeStampValidation.class);

	/**
	 * Checks the time-stamp tokens' digital signatures.
	 * 
	 * @param signatureTimeStamp
	 * @param signatureElement
	 * @return
	 * @throws XAdESValidationException
	 */
	public static List<TimeStampToken> verify(
			XAdESTimeStampType signatureTimeStamp, Element signatureElement)
			throws XAdESValidationException {

		LOG.debug("validate SignatureTimeStamp...");

		List<TimeStampToken> timeStampTokens = XAdESUtils
				.getTimeStampTokens(signatureTimeStamp);
		if (timeStampTokens.isEmpty()) {
			LOG.error("No timestamp tokens present in SignatureTimeStamp");
			throw new XAdESValidationException(
					"No timestamp tokens present in SignatureTimeStamp");
		}

		// 2. take ds:SignatureValue element
		NodeList signatureValueNodeList = signatureElement
				.getElementsByTagNameNS(XMLSignature.XMLNS, "SignatureValue");
		if (0 == signatureValueNodeList.getLength()) {
			LOG.error("no XML signature valuefound");
			throw new XAdESValidationException("no XML signature valuefound");
		}

		// 3. canonicalize using CanonicalizationMethod if any, else take dsig's
		TimeStampDigestInput digestInput = new TimeStampDigestInput(
				signatureTimeStamp.getCanonicalizationMethod().getAlgorithm());
		digestInput.addNode(signatureValueNodeList.item(0));

		for (TimeStampToken timeStampToken : timeStampTokens) {

			// 1. verify signature in timestamp token
			XAdESUtils.verifyTimeStampTokenSignature(timeStampToken);

			// 4. for-each timestamp token, compute digest and compare
			XAdESUtils.verifyTimeStampTokenDigest(timeStampToken, digestInput);
		}

		return timeStampTokens;
	}

}
