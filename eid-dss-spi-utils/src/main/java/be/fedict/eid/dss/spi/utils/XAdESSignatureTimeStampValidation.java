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
 * XAdES SignatureTimeStamp validator. Used by {@link XAdESValidation}.
 *
 * @author Wim Vandenhaute
 */
public class XAdESSignatureTimeStampValidation {

    private static final Log LOG = LogFactory.getLog(XAdESSignatureTimeStampValidation.class);

    public XAdESSignatureTimeStampValidation() {

    }

    public List<TimeStampToken> validate(XAdESTimeStampType signatureTimeStamp,
                                         Element signatureElement)
            throws TSPException, IOException, CMSException,
            NoSuchProviderException, NoSuchAlgorithmException, CertStoreException,
            CertificateExpiredException, CertificateNotYetValidException {

        List<TimeStampToken> timeStampTokens = XAdESUtils.getTimeStampTokens(signatureTimeStamp);

        // trust validation
        if (timeStampTokens.isEmpty()) {
            LOG.error("No timestamp tokens present in SignatureTimeStamp");
            throw new RuntimeException("No timestamp tokens present in SignatureTimeStamp");
        }

        // 2. take ds:SignatureValue element
        NodeList signatureValueNodeList = signatureElement.getElementsByTagNameNS(
                XMLSignature.XMLNS, "SignatureValue");
        if (0 == signatureValueNodeList.getLength()) {
            LOG.error("no XML signature valuefound");
            throw new RuntimeException("no XML signature valuefound");
        }

        // 3. canonicalize using CanonicalizationMethod if any, else take dsig's
        TimeStampDigestInput digestInput = new TimeStampDigestInput(
                signatureTimeStamp.getCanonicalizationMethod().getAlgorithm());
        digestInput.addNode(signatureValueNodeList.item(0));

        for (TimeStampToken timeStampToken : timeStampTokens) {

            // 1. verify signature in timestamp token
            XAdESUtils.validateTimeStampTokenSignature(timeStampToken);

            // 4. for-each timestamp token, compute digest and compare
            XAdESUtils.verifyTimeStampTokenDigest(timeStampToken, digestInput);

            /* 5. time coherence
            *
            * posterior to SigningTime and AllDataObjectsTimeStamp, IndividualDataObjectsTimeStamp, if present
            *
            * previous to times in tokens in RefsOnlyTimeStamp, SigAndRefsTimeStamp and ArchiveTimeStamp
            */
        }

        return timeStampTokens;
    }

}
