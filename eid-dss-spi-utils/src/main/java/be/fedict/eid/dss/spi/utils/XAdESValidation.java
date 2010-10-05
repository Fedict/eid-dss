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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;
import org.bouncycastle.ocsp.OCSPResp;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import be.fedict.eid.applet.service.signer.jaxb.xades132.AnyType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CRLValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CertIDListType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CertIDType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CertificateValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.ClaimedRolesListType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.DigestAlgAndValueType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.EncapsulatedPKIDataType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.OCSPValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.ObjectFactory;
import be.fedict.eid.applet.service.signer.jaxb.xades132.QualifyingPropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.RevocationValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.SignedPropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.SignedSignaturePropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.SignerRoleType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.UnsignedPropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.UnsignedSignaturePropertiesType;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.SignatureInfo;

/**
 * XAdES-X-L v1.4.1 validation utility class. Can be shared between different
 * document service implementations.
 * 
 * @author Frank Cornelis
 * 
 */
public class XAdESValidation {

	private static final Log LOG = LogFactory.getLog(XAdESValidation.class);

	private final Unmarshaller unmarshaller;

	private final CertificateFactory certificateFactory;

	private final DSSDocumentContext documentContext;

	public XAdESValidation(DSSDocumentContext documentContext) {
		this.documentContext = documentContext;

		JAXBContext jaxbContext;
		try {
			jaxbContext = JAXBContext
					.newInstance(
							ObjectFactory.class,
							be.fedict.eid.applet.service.signer.jaxb.xades141.ObjectFactory.class);
			this.unmarshaller = jaxbContext.createUnmarshaller();
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
		SignedInfo signedInfo = xmlSignature.getSignedInfo();
		List<Reference> references = signedInfo.getReferences();
		String xadesSignedPropertiesUri = null;
		for (Reference reference : references) {
			if ("http://uri.etsi.org/01903#SignedProperties".equals(reference
					.getType())) {
				xadesSignedPropertiesUri = reference.getURI();
				break;
			}
		}
		if (null == xadesSignedPropertiesUri) {
			LOG.error("no XAdES SignedProperties as part of signed XML data");
			throw new RuntimeException("no XAdES SignedProperties");
		}
		String xadesSignedPropertiesId = xadesSignedPropertiesUri.substring(1);
		Element nsElement = document.createElement("nsElement");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
				Constants.SignatureSpecNS);
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:xades",
				"http://uri.etsi.org/01903/v1.3.2#");
		Node xadesQualifyingPropertiesNode = XPathAPI.selectSingleNode(
				signatureElement,
				"ds:Object/xades:QualifyingProperties[xades:SignedProperties/@Id='"
						+ xadesSignedPropertiesId + "']", nsElement);

		JAXBElement<QualifyingPropertiesType> qualifyingPropertiesElement = (JAXBElement<QualifyingPropertiesType>) this.unmarshaller
				.unmarshal(xadesQualifyingPropertiesNode);
		QualifyingPropertiesType qualifyingProperties = qualifyingPropertiesElement
				.getValue();
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
		if (false == Arrays.equals(actualCertDigestValue, certDigestValue)) {
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
				if (false == claimedRoles.isEmpty()) {
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

		/*
		 * Retrieve certificate chain and revocation data from XAdES-X-L
		 * extension for trust validation.
		 */
		List<X509Certificate> certificateChain = new LinkedList<X509Certificate>();
		List<X509CRL> crls = new LinkedList<X509CRL>();
		List<OCSPResp> ocspResponses = new LinkedList<OCSPResp>();
		UnsignedPropertiesType unsignedProperties = qualifyingProperties
				.getUnsignedProperties();
		UnsignedSignaturePropertiesType unsignedSignatureProperties = unsignedProperties
				.getUnsignedSignatureProperties();
		List<Object> unsignedSignaturePropertiesContentList = unsignedSignatureProperties
				.getCounterSignatureOrSignatureTimeStampOrCompleteCertificateRefs();
		for (Object unsignedSignatureProperty : unsignedSignaturePropertiesContentList) {
			if (false == unsignedSignatureProperty instanceof JAXBElement) {
				continue;
			}
			JAXBElement<?> unsignedSignaturePropertyElement = (JAXBElement<?>) unsignedSignatureProperty;
			Object unsignedSignaturePropertyValue = unsignedSignaturePropertyElement
					.getValue();
			if (unsignedSignaturePropertyValue instanceof RevocationValuesType) {
				RevocationValuesType revocationValues = (RevocationValuesType) unsignedSignaturePropertyValue;
				CRLValuesType crlValues = revocationValues.getCRLValues();
				List<EncapsulatedPKIDataType> crlValuesList = crlValues
						.getEncapsulatedCRLValue();
				for (EncapsulatedPKIDataType crlValue : crlValuesList) {
					byte[] encodedCrl = crlValue.getValue();
					X509CRL crl = (X509CRL) this.certificateFactory
							.generateCRL(new ByteArrayInputStream(encodedCrl));
					crls.add(crl);
				}
				OCSPValuesType ocspValues = revocationValues.getOCSPValues();
				List<EncapsulatedPKIDataType> ocspValuesList = ocspValues
						.getEncapsulatedOCSPValue();
				for (EncapsulatedPKIDataType ocspValue : ocspValuesList) {
					byte[] encodedOcspResponse = ocspValue.getValue();
					OCSPResp ocspResp = new OCSPResp(encodedOcspResponse);
					ocspResponses.add(ocspResp);
				}
				continue;
			}
			if (unsignedSignaturePropertyValue instanceof CertificateValuesType) {
				CertificateValuesType certificateValues = (CertificateValuesType) unsignedSignaturePropertyValue;
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
						certificateChain.add(certificate);
					}
				}
			}
		}

		/*
		 * Check certificate chain is indeed contains the signing certificate.
		 */
		if (certificateChain.isEmpty()) {
			LOG.error("no certificate chain present in ds:KeyInfo");
			throw new RuntimeException("no cert chain in ds:KeyInfo");
		}
		if (false == Arrays.equals(signingCertificate.getEncoded(),
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

		SignatureInfo signatureInfo = new SignatureInfo(signingCertificate,
				signingTime, role);
		return signatureInfo;
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
}
