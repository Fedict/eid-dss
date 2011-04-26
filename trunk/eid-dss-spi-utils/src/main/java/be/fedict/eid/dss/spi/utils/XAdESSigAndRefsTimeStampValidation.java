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

import be.fedict.eid.applet.service.signer.jaxb.xades132.XAdESTimeStampType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.XMLSignature;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.util.List;

/**
 * XAdES SigAndRefsTimeStamp validator. Used by {@link be.fedict.eid.dss.spi.utils.XAdESValidation}.
 *
 * @author Wim Vandenhaute
 */
public abstract class XAdESSigAndRefsTimeStampValidation {

    private static final Log LOG = LogFactory.getLog(XAdESSigAndRefsTimeStampValidation.class);

    public static List<TimeStampToken> validate(XAdESTimeStampType sigAndRefsTimeStamp,
                                                Element signatureElement)
            throws TSPException, IOException, CMSException,
            NoSuchProviderException, NoSuchAlgorithmException, CertStoreException,
            CertificateExpiredException, CertificateNotYetValidException {

        LOG.debug("validate SigAndRefsTimeStamp...");

        List<TimeStampToken> timeStampTokens = XAdESUtils.getTimeStampTokens(sigAndRefsTimeStamp);
        if (timeStampTokens.isEmpty()) {
            LOG.error("No timestamp tokens present in SigAndRefsTimeStamp");
            throw new RuntimeException("No timestamp tokens present in SigAndRefsTimeStamp");
        }

        TimeStampDigestInput digestInput = new TimeStampDigestInput(
                sigAndRefsTimeStamp.getCanonicalizationMethod().getAlgorithm());

        /*
         * 2. check ds:SignatureValue present
         * 3. take ds:SignatureValue, cannonicalize and concatenate bytes.
         */
        NodeList signatureValueNodeList = signatureElement.getElementsByTagNameNS(
                XMLSignature.XMLNS, "SignatureValue");
        if (0 == signatureValueNodeList.getLength()) {
            LOG.error("no XML signature valuefound");
            throw new RuntimeException("no XML signature valuefound");
        }
        digestInput.addNode(signatureValueNodeList.item(0));

        /*
         *  4. check SignatureTimeStamp(s), CompleteCertificateRefs, CompleteRevocationRefs, AttributeCertificateRefs, AttributeRevocationRefs
         *  5. canonicalize these and concatenate to bytestream from step 3
         */
        addDigest(signatureElement, XAdESUtils.XADES_132_NS_URI,
                "SignatureTimeStamp", digestInput);
        addDigest(signatureElement, XAdESUtils.XADES_132_NS_URI,
                "CompleteCertificateRefs", digestInput);
        addDigest(signatureElement, XAdESUtils.XADES_132_NS_URI,
                "CompleteRevocationRefs", digestInput);


        for (TimeStampToken timeStampToken : timeStampTokens) {

            // 1. verify signature in timestamp token
            XAdESUtils.validateTimeStampTokenSignature(timeStampToken);

            // 6. compute digest and compare with token
            XAdESUtils.verifyTimeStampTokenDigest(timeStampToken, digestInput);
        }


        /*
         * 7. time coherence:
         *
         * posterior to SigningTime and AllDataObjectsTimeStamp, IndividualDataObjectsTimeStamp or SignatureTimeStamp,
         *
         * previous to times in tokens in ArchiveTimeStamp elements
         */

        return timeStampTokens;
    }

    private static void addDigest(Element signatureElement, String namespaceURI,
                                  String localName, TimeStampDigestInput digestInput) {

        NodeList nodeList = signatureElement.getElementsByTagNameNS(
                namespaceURI, localName);
        if (0 == nodeList.getLength()) {
            LOG.error("no " + localName + " element found");
            throw new RuntimeException("no " + localName + " element found");
        }
        for (int i = 0; i < nodeList.getLength(); i++) {
            digestInput.addNode(nodeList.item(i));
        }
    }

}
