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
import org.apache.xpath.XPathAPI;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.*;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * XAdES-X-L v1.4.1 validation utility class. Can be shared between different
 * document service implementations.
 *
 * @author Frank Cornelis
 */
public class XAdESValidation {

    private static final Log LOG = LogFactory.getLog(XAdESValidation.class);

    private final Unmarshaller xadesUnmarshaller;

    private final Unmarshaller identityUnmarshaller;

    private final CertificateFactory certificateFactory;

    private final DSSDocumentContext documentContext;

    public XAdESValidation(DSSDocumentContext documentContext) {
        this.documentContext = documentContext;

        try {
            JAXBContext xadesJaxbContext = JAXBContext
                    .newInstance(
                            ObjectFactory.class,
                            be.fedict.eid.applet.service.signer.jaxb.xades141.ObjectFactory.class);
            this.xadesUnmarshaller = xadesJaxbContext.createUnmarshaller();

            JAXBContext identityJaxbContext = JAXBContext
                    .newInstance(be.fedict.eid.applet.service.signer.jaxb.identity.ObjectFactory.class);
            this.identityUnmarshaller = identityJaxbContext
                    .createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("JAXB error: " + e.getMessage(), e);
        }

        try {
            this.certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new RuntimeException("certificate factory error: "
                    + e.getMessage(), e);
        }
    }

    public SignatureInfo validate(Document document, XMLSignature xmlSignature,
                                  Element signatureElement, X509Certificate signingCertificate)
            throws Exception {
        /*
         * Get signing time from XAdES-BES extension.
         */
        Element nsElement = getNsElement(document);

        QualifyingPropertiesType qualifyingProperties = getQualifyingProperties(
                nsElement, xmlSignature, signatureElement);
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


        // TODO: validate XAdES timestamps
        XAdESTimeStampType signatureTimeStamp = findUnsignedSignatureProperty(
                qualifyingProperties, XAdESTimeStampType.class, "SignatureTimeStamp");
        if (null == signatureTimeStamp) {
            LOG.error("No SignatureTimeStamp present");
            throw new RuntimeException("No SignatureTimeStamp present");
        }
        validateTimeStampTokenTrust(signatureTimeStamp);

        XAdESTimeStampType sigAndRefsTimeStamp = findUnsignedSignatureProperty(
                qualifyingProperties, XAdESTimeStampType.class, "SigAndRefsTimeStamp");
        if (null == sigAndRefsTimeStamp) {
            LOG.error("No SigAndRefsTimeStamp present");
            throw new RuntimeException("No SigAndRefsTimeStamp present");
        }
        validateTimeStampTokenTrust(sigAndRefsTimeStamp);


        /*
        * Retrieve certificate chain and revocation data from XAdES-X-L
        * extension for trust validation.
        */
        RevocationValuesType revocationValues = findUnsignedSignatureProperty(
                qualifyingProperties, RevocationValuesType.class);
        List<X509CRL> crls = getCrls(revocationValues);
        List<OCSPResp> ocspResponses = getOCSPResponses(revocationValues);

        CertificateValuesType certificateValues = findUnsignedSignatureProperty(
                qualifyingProperties, CertificateValuesType.class);
        if (null == certificateValues) {
            LOG.error("no CertificateValuesType element found.");
            throw new RuntimeException("no CertificateValuesType element found.");
        }
        List<X509Certificate> certificateChain = getCertificates(certificateValues);
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

        IdentityType identity = findIdentity(nsElement, xmlSignature,
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

    private void validateTimeStampTokenTrust(XAdESTimeStampType timeStampToken)
            throws Exception {

        if (timeStampToken.getEncapsulatedTimeStampOrXMLTimeStamp().isEmpty()) {
            LOG.error("No encapsulated timestamp present in timestamp token");
            throw new RuntimeException("No encapsulated timestamp present in timestamp token");
        }
        EncapsulatedPKIDataType encapsulatedTimeStampToken =
                (EncapsulatedPKIDataType) timeStampToken
                        .getEncapsulatedTimeStampOrXMLTimeStamp().get(0);
        byte[] encodedTimestampToken = encapsulatedTimeStampToken.getValue();
        TimeStampToken timestampToken = new TimeStampToken(new CMSSignedData(
                encodedTimestampToken));
        this.documentContext.validate(timestampToken);

    }

    @SuppressWarnings("unchecked")
    private IdentityType findIdentity(Element nsElement,
                                      XMLSignature xmlSignature,
                                      Element signatureElement)
            throws JAXBException, TransformerException {

        String identityUri = findReferenceUri(xmlSignature,
                IdentitySignatureFacet.REFERENCE_TYPE);
        if (null != identityUri) {
            String identityId = identityUri.substring(1);
            Node identityNode = XPathAPI.selectSingleNode(signatureElement,
                    "ds:Object/identity:Identity[@Id = '" + identityId + "']",
                    nsElement);
            if (null != identityNode) {
                JAXBElement<IdentityType> identityElement =
                        (JAXBElement<IdentityType>) this.identityUnmarshaller
                                .unmarshal(identityNode);
                return identityElement.getValue();
            }
        }
        return null;
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

    private List<X509Certificate> getCertificates(CertificateValuesType certificateValues)
            throws CertificateException {

        List<X509Certificate> certificates = new LinkedList<X509Certificate>();
        List<Object> certificateValuesContent = certificateValues
                .getEncapsulatedX509CertificateOrOtherCertificate();
        for (Object certificateValueContent : certificateValuesContent) {
            if (certificateValueContent instanceof EncapsulatedPKIDataType) {
                EncapsulatedPKIDataType encapsulatedPkiData = (EncapsulatedPKIDataType) certificateValueContent;
                byte[] encodedCertificate = encapsulatedPkiData
                        .getValue();
                X509Certificate certificate = (X509Certificate) this.certificateFactory
                        .generateCertificate(new ByteArrayInputStream(
                                encodedCertificate));
                certificates.add(certificate);
            }
        }
        return certificates;
    }

    private List<OCSPResp> getOCSPResponses(RevocationValuesType revocationValues)
            throws IOException {

        List<OCSPResp> ocspResponses = new LinkedList<OCSPResp>();
        OCSPValuesType ocspValues = revocationValues.getOCSPValues();
        List<EncapsulatedPKIDataType> ocspValuesList = ocspValues
                .getEncapsulatedOCSPValue();
        for (EncapsulatedPKIDataType ocspValue : ocspValuesList) {
            byte[] encodedOcspResponse = ocspValue.getValue();
            OCSPResp ocspResp = new OCSPResp(encodedOcspResponse);
            ocspResponses.add(ocspResp);
        }
        return ocspResponses;
    }

    private List<X509CRL> getCrls(RevocationValuesType revocationValues)
            throws CRLException {

        List<X509CRL> crls = new LinkedList<X509CRL>();
        CRLValuesType crlValues = revocationValues.getCRLValues();
        List<EncapsulatedPKIDataType> crlValuesList = crlValues
                .getEncapsulatedCRLValue();
        for (EncapsulatedPKIDataType crlValue : crlValuesList) {
            byte[] encodedCrl = crlValue.getValue();
            X509CRL crl = (X509CRL) this.certificateFactory
                    .generateCRL(new ByteArrayInputStream(encodedCrl));
            crls.add(crl);
        }
        return crls;
    }

    private <T> T findUnsignedSignatureProperty(QualifyingPropertiesType qualifyingProperties,
                                                Class<T> declaredType) {
        return findUnsignedSignatureProperty(qualifyingProperties, declaredType, null);
    }

    @SuppressWarnings("unchecked")
    private <T> T findUnsignedSignatureProperty(QualifyingPropertiesType qualifyingProperties,
                                                Class<T> declaredType, String name) {

        UnsignedPropertiesType unsignedProperties = qualifyingProperties
                .getUnsignedProperties();
        UnsignedSignaturePropertiesType unsignedSignatureProperties = unsignedProperties
                .getUnsignedSignatureProperties();
        List<Object> unsignedSignaturePropertiesContentList = unsignedSignatureProperties
                .getCounterSignatureOrSignatureTimeStampOrCompleteCertificateRefs();
        for (Object unsignedSignatureProperty : unsignedSignaturePropertiesContentList) {
            if (!(unsignedSignatureProperty instanceof JAXBElement)) {
                continue;
            }
            JAXBElement<?> unsignedSignaturePropertyElement = (JAXBElement<?>) unsignedSignatureProperty;
            Object unsignedSignaturePropertyValue = unsignedSignaturePropertyElement
                    .getValue();
            if (unsignedSignaturePropertyValue.getClass().isAssignableFrom(declaredType)) {

                if (null == name) {
                    return (T) unsignedSignaturePropertyValue;
                } else if (unsignedSignaturePropertyElement.getName().getLocalPart().equals(name)) {
                    return (T) unsignedSignaturePropertyValue;
                }
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private String findReferenceUri(XMLSignature xmlSignature, String type) {

        SignedInfo signedInfo = xmlSignature.getSignedInfo();
        List<Reference> references = signedInfo.getReferences();
        for (Reference reference : references) {
            if (type.equals(reference.getType())) {
                return reference.getURI();
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private QualifyingPropertiesType getQualifyingProperties(Element nsElement,
                                                             XMLSignature xmlSignature,
                                                             Element signatureElement)
            throws TransformerException, JAXBException {

        String xadesSignedPropertiesUri = findReferenceUri(xmlSignature,
                "http://uri.etsi.org/01903#SignedProperties");
        if (null == xadesSignedPropertiesUri) {
            LOG.error("no XAdES SignedProperties as part of signed XML data");
            throw new RuntimeException("no XAdES SignedProperties");
        }

        String xadesSignedPropertiesId = xadesSignedPropertiesUri.substring(1);
        Node xadesQualifyingPropertiesNode = XPathAPI.selectSingleNode(
                signatureElement,
                "ds:Object/xades:QualifyingProperties[xades:SignedProperties/@Id='"
                        + xadesSignedPropertiesId + "']", nsElement);

        JAXBElement<QualifyingPropertiesType> qualifyingPropertiesElement =
                (JAXBElement<QualifyingPropertiesType>) this.xadesUnmarshaller
                        .unmarshal(xadesQualifyingPropertiesNode);
        return qualifyingPropertiesElement.getValue();
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
