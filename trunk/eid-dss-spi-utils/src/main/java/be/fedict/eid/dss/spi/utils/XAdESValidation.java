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

import be.fedict.eid.applet.service.signer.facets.IdentitySignatureFacet;
import be.fedict.eid.applet.service.signer.jaxb.identity.IdentityType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.*;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.SignatureInfo;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Constants;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.datatype.XMLGregorianCalendar;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * XAdES-X-L v1.4.1 validation utility class. Can be shared between different
 * document service implementations.
 *
 * @author Frank Cornelis
 */
public class XAdESValidation {

    private static final Log LOG = LogFactory.getLog(XAdESValidation.class);

    private final DSSDocumentContext documentContext;

    public XAdESValidation(DSSDocumentContext documentContext) {
        this.documentContext = documentContext;
    }

    public SignatureInfo validate(Document document, XMLSignature xmlSignature,
                                  Element signatureElement, X509Certificate signingCertificate)
            throws Exception {
        /*
         * Get signing time from XAdES-BES extension.
         */
        Element nsElement = getNsElement(document);

        QualifyingPropertiesType qualifyingProperties =
                XAdESUtils.getQualifyingProperties(nsElement, xmlSignature,
                        signatureElement);
        SignedPropertiesType signedProperties = qualifyingProperties
                .getSignedProperties();
        SignedSignaturePropertiesType signedSignatureProperties = signedProperties
                .getSignedSignatureProperties();
        XMLGregorianCalendar signingTimeXMLGregorianCalendar = signedSignatureProperties
                .getSigningTime();
        Date signingTime = signingTimeXMLGregorianCalendar
                .toGregorianCalendar().getTime();
        LOG.debug("XAdES signing time: " + signingTime);

        /*
         * Check the XAdES signing certificate
         */
        CertIDListType signingCertificateCertIDList = signedSignatureProperties
                .getSigningCertificate();
        List<CertIDType> signingCertificateCertIDs = signingCertificateCertIDList
                .getCert();
        CertIDType signingCertificateCertID = signingCertificateCertIDs.get(0);
        DigestAlgAndValueType signingCertificateDigestAlgAndValue = signingCertificateCertID
                .getCertDigest();
        String certXmlDigestAlgo = signingCertificateDigestAlgAndValue
                .getDigestMethod().getAlgorithm();
        String certDigestAlgo = getDigestAlgo(certXmlDigestAlgo);
        byte[] certDigestValue = signingCertificateDigestAlgAndValue
                .getDigestValue();
        MessageDigest messageDigest;
        try {
            messageDigest = MessageDigest.getInstance(certDigestAlgo);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("message digest algo error: "
                    + e.getMessage(), e);
        }
        byte[] actualCertDigestValue = messageDigest.digest(signingCertificate
                .getEncoded());
        if (!Arrays.equals(actualCertDigestValue, certDigestValue)) {
            throw new RuntimeException(
                    "XAdES signing certificate not corresponding with actual signing certificate");
        }
        LOG.debug("XAdES signing certificate OK");

        /*
         * Get XAdES ClaimedRole.
         */
        String role = null;
        SignerRoleType signerRole = signedSignatureProperties.getSignerRole();
        if (null != signerRole) {
            ClaimedRolesListType claimedRolesList = signerRole
                    .getClaimedRoles();
            if (null != claimedRolesList) {
                List<AnyType> claimedRoles = claimedRolesList.getClaimedRole();
                if (!claimedRoles.isEmpty()) {
                    AnyType claimedRole = claimedRoles.get(0);
                    List<Object> claimedRoleContent = claimedRole.getContent();
                    for (Object claimedRoleContentItem : claimedRoleContent) {
                        if (claimedRoleContentItem instanceof String) {
                            role = (String) claimedRoleContentItem;
                            LOG.debug("XAdES claimed role: " + role);
                            break;
                        }
                    }
                }
            }
        }


        // validate SigAndRefsTimeStamp
        XAdESTimeStampType sigAndRefsTimeStamp = XAdESUtils.findUnsignedSignatureProperty(
                qualifyingProperties, XAdESTimeStampType.class, "SigAndRefsTimeStamp");
        if (null == sigAndRefsTimeStamp) {
            LOG.error("No SigAndRefsTimeStamp present");
            throw new RuntimeException("No SigAndRefsTimeStamp present");
        }
        validateSigAndRefsTimeStamp(sigAndRefsTimeStamp);

        // validate SignatureTimeStamp
        XAdESTimeStampType signatureTimeStamp = XAdESUtils.findUnsignedSignatureProperty(
                qualifyingProperties, XAdESTimeStampType.class, "SignatureTimeStamp");
        if (null == signatureTimeStamp) {
            LOG.error("No SignatureTimeStamp present");
            throw new RuntimeException("No SignatureTimeStamp present");
        }

        List<TimeStampToken> signatureTimeStampTokens =
                new XAdESSignatureTimeStampValidation().validate(
                        signatureTimeStamp, signatureElement);

        // trust validation
        for (TimeStampToken signatureTimeStampToken : signatureTimeStampTokens) {
            this.documentContext.validate(signatureTimeStampToken);
        }

        // TODO: time coherence validation


        /*
        * Retrieve certificate chain and revocation data from XAdES-X-L
        * extension for trust validation.
        */
        RevocationValuesType revocationValues = XAdESUtils.findUnsignedSignatureProperty(
                qualifyingProperties, RevocationValuesType.class);
        List<X509CRL> crls = XAdESUtils.getCrls(revocationValues);
        List<OCSPResp> ocspResponses = XAdESUtils.getOCSPResponses(revocationValues);

        CertificateValuesType certificateValues = XAdESUtils.findUnsignedSignatureProperty(
                qualifyingProperties, CertificateValuesType.class);
        if (null == certificateValues) {
            LOG.error("no CertificateValuesType element found.");
            throw new RuntimeException("no CertificateValuesType element found.");
        }
        List<X509Certificate> certificateChain = XAdESUtils.getCertificates(certificateValues);
        if (certificateChain.isEmpty()) {
            LOG.error("no certificate chain present in CertificateValuesType");
            throw new RuntimeException("no cert chain in CertificateValuesType");
        }

        /*
         * Check certificate chain is indeed contains the signing certificate.
         */
        if (!Arrays.equals(signingCertificate.getEncoded(),
                certificateChain.get(0).getEncoded())) {
            throw new RuntimeException(
                    "XAdES certificate chain does not include actual signing certificate");
        }
        LOG.debug("XAdES certificate chain contains actual signing certificate");

        /*
         * Perform trust validation via eID Trust Service
         */
        this.documentContext.validate(certificateChain, signingTime,
                ocspResponses, crls);

        /*
         * Retrieve the possible eID identity signature extension data.
         */
        String firstName = null;
        String name = null;
        String middleName = null;
        SignatureInfo.Gender gender = null;
        byte[] photo = null;

        IdentityType identity = XAdESUtils.findIdentity(nsElement, xmlSignature,
                signatureElement);
        if (null != identity) {
            firstName = identity.getFirstName();
            name = identity.getName();
            middleName = identity.getMiddleName();
            switch (identity.getGender()) {
                case MALE:
                    gender = SignatureInfo.Gender.MALE;
                    break;
                case FEMALE:
                    gender = SignatureInfo.Gender.FEMALE;
                    break;
            }
            photo = identity.getPhoto().getValue();
        }

        /*
         * Return the result of the signature analysis.
         */
        return new SignatureInfo(signingCertificate,
                signingTime, role, firstName, name, middleName, gender, photo);
    }

    private List<TimeStampToken> validateSigAndRefsTimeStamp(XAdESTimeStampType sigAndRefsTimeStamp)
            throws Exception {

        List<TimeStampToken> timeStampTokens = XAdESUtils.getTimeStampTokens(sigAndRefsTimeStamp);

        // trust validation
        if (timeStampTokens.isEmpty()) {
            LOG.error("No timestamp tokens present in SignatureTimeStamp");
            throw new RuntimeException("No timestamp tokens present in SignatureTimeStamp");
        }
        for (TimeStampToken timeStampToken : timeStampTokens) {
            this.documentContext.validate(timeStampToken);
        }

        // TODO: validate SigAndRefsTimeStamp

        // 1. verify signature in timestamp token

        // 2. check all timestamped signed properties and regular elements present

        // 3. take ds:SignatureValue, cannonicalize and concatenate bytes.

        /*
         * 4.   check CompleteCertificateRefs, CompleteRevocationRefs present in XAdES signature
         *      check SignatureTimeStamp, AttributeCertificateRefs and AttributeRevocationRefs appear before SigAndRefsTimeStamp
         */

        /*
         * 5. take following unsigned properties, canonicalize and concatenate bytes to bytestream from step 3.
         *
         * CompleteCertificateRefs, CompleteRevocationRefs, SignatureTimeStamp, AttributeCertificateRefs and AttributeRevocationRefs
         */

        // 6. compute digest and compare with token

        /*
         * 7. time coherence:
         *
         * posterior to SigningTime and AllDataObjectsTimeStamp, IndividualDataObjectsTimeStamp or SignatureTimeStamp,
         *
         * previous to times in tokens in ArchiveTimeStamp elements
         */

        return timeStampTokens;
    }

    public static String getDigestAlgo(String xmlDigestAlgo) {
        if (DigestMethod.SHA1.equals(xmlDigestAlgo)) {
            return "SHA-1";
        }
        if (DigestMethod.SHA256.equals(xmlDigestAlgo)) {
            return "SHA-256";
        }
        if (DigestMethod.SHA512.equals(xmlDigestAlgo)) {
            return "SHA-512";
        }
        throw new RuntimeException("unsupported XML digest algo: "
                + xmlDigestAlgo);
    }


    private Element getNsElement(Document document) {

        Element nsElement = document.createElement("nsElement");
        nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
                Constants.SignatureSpecNS);
        nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:xades",
                "http://uri.etsi.org/01903/v1.3.2#");
        nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:identity",
                IdentitySignatureFacet.NAMESPACE_URI);
        return nsElement;
    }
}
