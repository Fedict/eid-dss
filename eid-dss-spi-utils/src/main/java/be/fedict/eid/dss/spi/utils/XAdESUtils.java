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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xpath.XPathAPI;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TSPException;
import org.bouncycastle.tsp.TimeStampToken;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Some XAdES Utility methods
 *
 * @author Wim Vandenhaute
 */
public abstract class XAdESUtils {

    private static final Log LOG = LogFactory.getLog(XAdESUtils.class);

    public static final String XADES_132_NS_URI = "http://uri.etsi.org/01903/v1.3.2#";
    public static final String XADES_141_NS_URI = "http://uri.etsi.org/01903/v1.4.1#";

    private static final CertificateFactory certificateFactory;
    private static final Unmarshaller xadesUnmarshaller;
    private static final Unmarshaller identityUnmarshaller;

    static {

        try {
            JAXBContext xadesJaxbContext = JAXBContext
                    .newInstance(
                            ObjectFactory.class,
                            be.fedict.eid.applet.service.signer.jaxb.xades141.ObjectFactory.class);
            xadesUnmarshaller = xadesJaxbContext.createUnmarshaller();

            JAXBContext identityJaxbContext = JAXBContext
                    .newInstance(be.fedict.eid.applet.service.signer.jaxb.identity.ObjectFactory.class);
            identityUnmarshaller = identityJaxbContext
                    .createUnmarshaller();
        } catch (JAXBException e) {
            throw new RuntimeException("JAXB error: " + e.getMessage(), e);
        }

        try {
            certificateFactory = CertificateFactory.getInstance("X.509");
        } catch (CertificateException e) {
            throw new RuntimeException("certificate factory error: "
                    + e.getMessage(), e);
        }
    }


    public static List<TimeStampToken> getTimeStampTokens(XAdESTimeStampType xadesTimeStamp)
            throws CMSException, TSPException, IOException {

        List<TimeStampToken> timeStampTokens = new LinkedList<TimeStampToken>();
        for (Object timeStampTokenObject : xadesTimeStamp.getEncapsulatedTimeStampOrXMLTimeStamp()) {

            if (timeStampTokenObject instanceof EncapsulatedPKIDataType) {

                EncapsulatedPKIDataType encapsulatedTimeStampToken =
                        (EncapsulatedPKIDataType) timeStampTokenObject;
                byte[] encodedTimestampToken = encapsulatedTimeStampToken.getValue();
                timeStampTokens.add(new TimeStampToken(new CMSSignedData(
                        encodedTimestampToken)));

            } else {
                throw new RuntimeException("Timestamp token of type: " +
                        timeStampTokenObject.getClass() + " not supported.");
            }
        }

        return timeStampTokens;
    }

    public static void validateTimeStampTokenSignature(TimeStampToken timeStampToken)
            throws CMSException, NoSuchProviderException,
            NoSuchAlgorithmException, CertStoreException,
            CertificateExpiredException, TSPException,
            CertificateNotYetValidException {

        List<X509Certificate> certificateChain = new LinkedList<X509Certificate>();
        CertStore certStore = timeStampToken.getCertificatesAndCRLs(
                "Collection", "BC");
        Collection<? extends Certificate> certificates = certStore
                .getCertificates(null);
        for (Certificate certificate : certificates) {
            certificateChain.add((X509Certificate) certificate);
        }
        X509Certificate tsaCertificate = certificateChain.get(certificateChain.size() - 1);

        timeStampToken.validate(tsaCertificate, "BC");
    }

    public static void verifyTimeStampTokenDigest(TimeStampToken timeStampToken,
                                                  TimeStampDigestInput digestInput) {

        LOG.debug("digest verification: algo=" +
                timeStampToken.getTimeStampInfo().getMessageImprintAlgOID());
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(
                    timeStampToken.getTimeStampInfo().getMessageImprintAlgOID());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

        if (!Arrays.equals(md.digest(digestInput.getBytes()),
                timeStampToken.getTimeStampInfo().getMessageImprintDigest())) {
            throw new RuntimeException("Digest verification failure for " +
                    "timestamp token in SignatureTimeStamp");
        }
    }

    public static List<X509Certificate> getCertificates(CertificateValuesType certificateValues)
            throws CertificateException {

        List<X509Certificate> certificates = new LinkedList<X509Certificate>();
        List<Object> certificateValuesContent = certificateValues
                .getEncapsulatedX509CertificateOrOtherCertificate();
        for (Object certificateValueContent : certificateValuesContent) {
            if (certificateValueContent instanceof EncapsulatedPKIDataType) {
                EncapsulatedPKIDataType encapsulatedPkiData = (EncapsulatedPKIDataType) certificateValueContent;
                byte[] encodedCertificate = encapsulatedPkiData
                        .getValue();
                X509Certificate certificate = (X509Certificate) certificateFactory
                        .generateCertificate(new ByteArrayInputStream(
                                encodedCertificate));
                certificates.add(certificate);
            }
        }
        return certificates;
    }

    public static List<OCSPResp> getOCSPResponses(RevocationValuesType revocationValues)
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

    public static List<X509CRL> getCrls(RevocationValuesType revocationValues)
            throws CRLException {

        List<X509CRL> crls = new LinkedList<X509CRL>();
        CRLValuesType crlValues = revocationValues.getCRLValues();
        List<EncapsulatedPKIDataType> crlValuesList = crlValues
                .getEncapsulatedCRLValue();
        for (EncapsulatedPKIDataType crlValue : crlValuesList) {
            byte[] encodedCrl = crlValue.getValue();
            X509CRL crl = (X509CRL) certificateFactory
                    .generateCRL(new ByteArrayInputStream(encodedCrl));
            crls.add(crl);
        }
        return crls;
    }

    public static <T> T findUnsignedSignatureProperty(QualifyingPropertiesType qualifyingProperties,
                                                      Class<T> declaredType) {
        return findUnsignedSignatureProperty(qualifyingProperties, declaredType, null);
    }

    @SuppressWarnings("unchecked")
    public static <T> T findUnsignedSignatureProperty(QualifyingPropertiesType qualifyingProperties,
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
    public static String findReferenceUri(XMLSignature xmlSignature, String type) {

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
    public static QualifyingPropertiesType getQualifyingProperties(Element nsElement,
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
                (JAXBElement<QualifyingPropertiesType>) xadesUnmarshaller
                        .unmarshal(xadesQualifyingPropertiesNode);
        return qualifyingPropertiesElement.getValue();
    }

    @SuppressWarnings("unchecked")
    public static IdentityType findIdentity(Element nsElement,
                                            XMLSignature xmlSignature,
                                            Element signatureElement)
            throws JAXBException, TransformerException {

        String identityUri = XAdESUtils.findReferenceUri(xmlSignature,
                IdentitySignatureFacet.REFERENCE_TYPE);
        if (null != identityUri) {
            String identityId = identityUri.substring(1);
            Node identityNode = XPathAPI.selectSingleNode(signatureElement,
                    "ds:Object/identity:Identity[@Id = '" + identityId + "']",
                    nsElement);
            if (null != identityNode) {
                JAXBElement<IdentityType> identityElement =
                        (JAXBElement<IdentityType>) identityUnmarshaller
                                .unmarshal(identityNode);
                return identityElement.getValue();
            }
        }
        return null;
    }

}
