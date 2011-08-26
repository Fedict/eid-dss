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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CRLException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.security.auth.x500.X500Principal;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.transform.TransformerException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xpath.XPathAPI;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import be.fedict.eid.applet.service.signer.facets.IdentitySignatureFacet;
import be.fedict.eid.applet.service.signer.jaxb.identity.IdentityType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CRLValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CertificateValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.EncapsulatedPKIDataType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.OCSPValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.ObjectFactory;
import be.fedict.eid.applet.service.signer.jaxb.xades132.QualifyingPropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.RevocationValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.UnsignedPropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.UnsignedSignaturePropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.XAdESTimeStampType;
import be.fedict.eid.dss.spi.utils.exception.XAdESValidationException;

/**
 * Some XAdES Utility methods
 * 
 * @author Wim Vandenhaute
 * @author Frank Cornelis
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
			identityUnmarshaller = identityJaxbContext.createUnmarshaller();
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

	/**
	 * Gives back all time-stamp tokens embedded within the given XAdES
	 * time-stamp container.
	 * 
	 * @param xadesTimeStamp
	 * @return
	 * @throws XAdESValidationException
	 */
	public static List<TimeStampToken> getTimeStampTokens(
			XAdESTimeStampType xadesTimeStamp) throws XAdESValidationException {

		try {
			List<TimeStampToken> timeStampTokens = new LinkedList<TimeStampToken>();
			for (Object timeStampTokenObject : xadesTimeStamp
					.getEncapsulatedTimeStampOrXMLTimeStamp()) {

				if (timeStampTokenObject instanceof EncapsulatedPKIDataType) {

					EncapsulatedPKIDataType encapsulatedTimeStampToken = (EncapsulatedPKIDataType) timeStampTokenObject;
					byte[] encodedTimestampToken = encapsulatedTimeStampToken
							.getValue();
					timeStampTokens.add(new TimeStampToken(new CMSSignedData(
							encodedTimestampToken)));

				} else {
					throw new XAdESValidationException(
							"Timestamp token of type: "
									+ timeStampTokenObject.getClass()
									+ " not supported.");
				}
			}

			return timeStampTokens;
		} catch (Exception e) {
			throw new XAdESValidationException(e);
		}
	}

	public static void validateTimeStampTokenSignature(
			TimeStampToken timeStampToken) throws XAdESValidationException {

		try {
			SignerId signerId = timeStampToken.getSID();
			BigInteger signerCertSerialNumber = signerId.getSerialNumber();
			X500Principal signerCertIssuer = signerId.getIssuer();

			CertStore certStore = timeStampToken.getCertificatesAndCRLs(
					"Collection", BouncyCastleProvider.PROVIDER_NAME);
			Collection<? extends Certificate> certificates = certStore
					.getCertificates(null);
			X509Certificate tsaCertificate = null;
			for (Certificate certificate : certificates) {
				X509Certificate x509Certificate = (X509Certificate) certificate;
				if (signerCertIssuer.equals(x509Certificate
						.getIssuerX500Principal())
						&& signerCertSerialNumber.equals(x509Certificate
								.getSerialNumber())) {
					tsaCertificate = x509Certificate;
					break;
				}
			}

			if (null == tsaCertificate) {
				throw new XAdESValidationException(
						"TSA certificate not present in TST");
			}

			timeStampToken.validate(tsaCertificate,
					BouncyCastleProvider.PROVIDER_NAME);
		} catch (Exception e) {
			throw new XAdESValidationException(e);
		}
	}

	public static void verifyTimeStampTokenDigest(
			TimeStampToken timeStampToken, TimeStampDigestInput digestInput)
			throws XAdESValidationException {

		LOG.debug("digest verification: algo="
				+ timeStampToken.getTimeStampInfo().getMessageImprintAlgOID());
		MessageDigest md;
		try {
			md = MessageDigest.getInstance(timeStampToken.getTimeStampInfo()
					.getMessageImprintAlgOID());
		} catch (NoSuchAlgorithmException e) {
			throw new XAdESValidationException(e);
		}
		// LOG.debug("digest input: " + new String(digestInput.getBytes()));

		if (!Arrays.equals(md.digest(digestInput.getBytes()), timeStampToken
				.getTimeStampInfo().getMessageImprintDigest())) {
			throw new XAdESValidationException(
					"Digest verification failure for " + "timestamp token");
		}
	}

	public static List<X509Certificate> getCertificates(
			CertificateValuesType certificateValues)
			throws XAdESValidationException {

		try {
			List<X509Certificate> certificates = new LinkedList<X509Certificate>();
			List<Object> certificateValuesContent = certificateValues
					.getEncapsulatedX509CertificateOrOtherCertificate();
			for (Object certificateValueContent : certificateValuesContent) {
				if (certificateValueContent instanceof EncapsulatedPKIDataType) {
					EncapsulatedPKIDataType encapsulatedPkiData = (EncapsulatedPKIDataType) certificateValueContent;
					byte[] encodedCertificate = encapsulatedPkiData.getValue();
					X509Certificate certificate = (X509Certificate) certificateFactory
							.generateCertificate(new ByteArrayInputStream(
									encodedCertificate));
					certificates.add(certificate);
				}
			}
			return certificates;
		} catch (CertificateException e) {
			throw new XAdESValidationException(e);
		}
	}

	public static List<OCSPResp> getOCSPResponses(
			RevocationValuesType revocationValues)
			throws XAdESValidationException {

		try {
			List<OCSPResp> ocspResponses = new LinkedList<OCSPResp>();
			OCSPValuesType ocspValues = revocationValues.getOCSPValues();
			if (null == ocspValues) {
				return ocspResponses;
			}
			List<EncapsulatedPKIDataType> ocspValuesList = ocspValues
					.getEncapsulatedOCSPValue();
			for (EncapsulatedPKIDataType ocspValue : ocspValuesList) {
				byte[] encodedOcspResponse = ocspValue.getValue();
				OCSPResp ocspResp = new OCSPResp(encodedOcspResponse);
				ocspResponses.add(ocspResp);
			}
			return ocspResponses;
		} catch (IOException e) {
			throw new XAdESValidationException(e);
		}
	}

	public static List<X509CRL> getCrls(RevocationValuesType revocationValues)
			throws XAdESValidationException {

		try {
			List<X509CRL> crls = new LinkedList<X509CRL>();
			CRLValuesType crlValues = revocationValues.getCRLValues();
			if (null == crlValues) {
				return crls;
			}
			List<EncapsulatedPKIDataType> crlValuesList = crlValues
					.getEncapsulatedCRLValue();
			for (EncapsulatedPKIDataType crlValue : crlValuesList) {
				byte[] encodedCrl = crlValue.getValue();
				X509CRL crl = (X509CRL) certificateFactory
						.generateCRL(new ByteArrayInputStream(encodedCrl));
				crls.add(crl);
			}
			return crls;
		} catch (CRLException e) {
			throw new XAdESValidationException(e);
		}
	}

	public static <T> T findUnsignedSignatureProperty(
			QualifyingPropertiesType qualifyingProperties, Class<T> declaredType) {
		return findUnsignedSignatureProperty(qualifyingProperties,
				declaredType, null);
	}

	@SuppressWarnings("unchecked")
	public static <T> T findUnsignedSignatureProperty(
			QualifyingPropertiesType qualifyingProperties,
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
			if (unsignedSignaturePropertyValue.getClass().isAssignableFrom(
					declaredType)) {

				if (null == name) {
					return (T) unsignedSignaturePropertyValue;
				} else if (unsignedSignaturePropertyElement.getName()
						.getLocalPart().equals(name)) {
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
	public static QualifyingPropertiesType getQualifyingProperties(
			Element nsElement, XMLSignature xmlSignature,
			Element signatureElement) throws XAdESValidationException {

		try {
			String xadesSignedPropertiesUri = findReferenceUri(xmlSignature,
					"http://uri.etsi.org/01903#SignedProperties");
			if (null == xadesSignedPropertiesUri) {
				LOG.error("no XAdES SignedProperties as part of signed XML data");
				throw new XAdESValidationException("no XAdES SignedProperties");
			}

			String xadesSignedPropertiesId = xadesSignedPropertiesUri
					.substring(1);
			Node xadesQualifyingPropertiesNode = XPathAPI.selectSingleNode(
					signatureElement,
					"ds:Object/xades:QualifyingProperties[xades:SignedProperties/@Id='"
							+ xadesSignedPropertiesId + "']", nsElement);

			JAXBElement<QualifyingPropertiesType> qualifyingPropertiesElement = (JAXBElement<QualifyingPropertiesType>) xadesUnmarshaller
					.unmarshal(xadesQualifyingPropertiesNode);
			return qualifyingPropertiesElement.getValue();
		} catch (TransformerException e) {
			throw new XAdESValidationException(e);
		} catch (JAXBException e) {
			throw new XAdESValidationException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T unmarshall(Element xadesElement, Class<T> xadesType)
			throws XAdESValidationException {
		JAXBElement<T> jaxbElement;
		try {
			jaxbElement = (JAXBElement<T>) xadesUnmarshaller
					.unmarshal(xadesElement);
		} catch (JAXBException e) {
			throw new XAdESValidationException(e);
		}
		T value = jaxbElement.getValue();
		return value;
	}

	public static Element findQualifyingPropertiesElement(Element nsElement,
			XMLSignature xmlSignature, Element signatureElement)
			throws XAdESValidationException {
		String xadesSignedPropertiesUri = findReferenceUri(xmlSignature,
				"http://uri.etsi.org/01903#SignedProperties");
		if (null == xadesSignedPropertiesUri) {
			LOG.error("no XAdES SignedProperties as part of signed XML data");
			throw new XAdESValidationException("no XAdES SignedProperties");
		}
		String xadesSignedPropertiesId = xadesSignedPropertiesUri.substring(1);
		Node xadesQualifyingPropertiesNode;
		try {
			xadesQualifyingPropertiesNode = XPathAPI.selectSingleNode(
					signatureElement,
					"ds:Object/xades:QualifyingProperties[xades:SignedProperties/@Id='"
							+ xadesSignedPropertiesId + "']", nsElement);
		} catch (TransformerException e) {
			throw new XAdESValidationException(e);
		}
		return (Element) xadesQualifyingPropertiesNode;
	}

	@SuppressWarnings("unchecked")
	public static IdentityType findIdentity(Element nsElement,
			XMLSignature xmlSignature, Element signatureElement)
			throws XAdESValidationException {

		try {
			String identityUri = XAdESUtils.findReferenceUri(xmlSignature,
					IdentitySignatureFacet.REFERENCE_TYPE);
			if (null != identityUri) {
				String identityId = identityUri.substring(1);
				Node identityNode = XPathAPI.selectSingleNode(signatureElement,
						"ds:Object[@Id = '" + identityId
								+ "']/identity:Identity", nsElement);
				if (null != identityNode) {
					JAXBElement<IdentityType> identityElement = (JAXBElement<IdentityType>) identityUnmarshaller
							.unmarshal(identityNode);
					return identityElement.getValue();
				}
			}
			return null;
		} catch (TransformerException e) {
			throw new XAdESValidationException(e);
		} catch (JAXBException e) {
			throw new XAdESValidationException(e);
		}

	}

	/**
	 * Finds a XAdES named unsigned signature property as DOM element.
	 * <p/>
	 * Working DOM based is required if you want to be able to find the next
	 * sibling at the DOM level. JAXB does not expose this properly.
	 * 
	 * @param qualifyingPropertiesElement
	 *            the XAdES qualifying properties DOM element.
	 * @param localName
	 * @return
	 * @throws XAdESValidationException
	 */
	public static Element findUnsignedSignaturePropertyElement(
			Element qualifyingPropertiesElement, String localName)
			throws XAdESValidationException {
		NodeList unsignedSignaturePropertiesNodeList = qualifyingPropertiesElement
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
			Element childElement = (Element) childNode;
			if (!XAdESUtils.XADES_132_NS_URI
					.equals(childNode.getNamespaceURI())) {
				continue;
			}
			String actualLocalName = childNode.getLocalName();
			if (localName.equals(actualLocalName)) {
				return childElement;
			}
		}
		return null;
	}

	/**
	 * Find the next sibling at DOM level of the given XAdES DOM element.
	 * 
	 * @param xadesElement
	 * @param namespace
	 * @param localName
	 * @param jaxbType
	 * @return
	 * @throws XAdESValidationException
	 */
	public static <T> T findNextSibling(Element xadesElement, String namespace,
			String localName, Class<T> jaxbType)
			throws XAdESValidationException {
		Node siblingNode = xadesElement.getNextSibling();
		while (siblingNode != null
				&& siblingNode.getNodeType() != Node.ELEMENT_NODE) {
			/*
			 * Can happen as shown during latest ETSI XAdES plugtests.
			 */
			LOG.debug("skipping a non-Element sibling: "
					+ siblingNode.getNodeType());
			if (Node.TEXT_NODE == siblingNode.getNodeType()) {
				LOG.debug("TEXT node sibling: \"" + siblingNode.getNodeValue()
						+ "\"");
			}
			siblingNode = siblingNode.getNextSibling();
		}
		if (null == siblingNode) {
			return null;
		}
		Element element = (Element) siblingNode;
		if (false == namespace.equals(element.getNamespaceURI())) {
			return null;
		}
		if (false == localName.equals(element.getLocalName())) {
			return null;
		}
		return unmarshall(element, jaxbType);
	}
}
