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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Constants;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;
import org.joda.time.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import be.fedict.eid.applet.service.signer.facets.IdentitySignatureFacet;
import be.fedict.eid.applet.service.signer.jaxb.identity.IdentityType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.AnyType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CertIDListType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CertIDType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CertificateValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.ClaimedRolesListType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.DigestAlgAndValueType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.QualifyingPropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.RevocationValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.SignedPropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.SignedSignaturePropertiesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.SignerRoleType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.XAdESTimeStampType;
import be.fedict.eid.applet.service.signer.jaxb.xades141.ValidationDataType;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.SignatureInfo;
import be.fedict.eid.dss.spi.utils.exception.XAdESValidationException;

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
			throws XAdESValidationException {

		try {
			/*
			 * Get signing time from XAdES-BES extension.
			 */
			Element nsElement = getNsElement(document);

			Element qualifyingPropertiesElement = XAdESUtils
					.findQualifyingPropertiesElement(nsElement, xmlSignature,
							signatureElement);
			if (null == qualifyingPropertiesElement) {
				throw new XAdESValidationException(
						"no matching xades:QualifyingProperties present");
			}
			QualifyingPropertiesType qualifyingProperties = XAdESUtils
					.unmarshall(qualifyingPropertiesElement,
							QualifyingPropertiesType.class);
			SignedPropertiesType signedProperties = qualifyingProperties
					.getSignedProperties();
			SignedSignaturePropertiesType signedSignatureProperties = signedProperties
					.getSignedSignatureProperties();
			XMLGregorianCalendar signingTimeXMLGregorianCalendar = signedSignatureProperties
					.getSigningTime();
			DateTime signingTime = new DateTime(signingTimeXMLGregorianCalendar
					.toGregorianCalendar().getTime());
			LOG.debug("XAdES signing time: " + signingTime);

			/*
			 * Check the XAdES signing certificate
			 */
			checkSigningCertificate(signingCertificate,
					signedSignatureProperties);

			/*
			 * Get XAdES ClaimedRole.
			 */
			String role = null;
			SignerRoleType signerRole = signedSignatureProperties
					.getSignerRole();
			if (null != signerRole) {
				ClaimedRolesListType claimedRolesList = signerRole
						.getClaimedRoles();
				if (null != claimedRolesList) {
					List<AnyType> claimedRoles = claimedRolesList
							.getClaimedRole();
					if (!claimedRoles.isEmpty()) {
						AnyType claimedRole = claimedRoles.get(0);
						List<Object> claimedRoleContent = claimedRole
								.getContent();
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

			// XAdES-T

			// validate SignatureTimeStamp
			Element signatureTimeStampElement = XAdESUtils
					.findUnsignedSignaturePropertyElement(
							qualifyingPropertiesElement, "SignatureTimeStamp");
			if (null == signatureTimeStampElement) {
				throw new XAdESValidationException(
						"no xades:SignatureTimeStamp present");
			}
			XAdESTimeStampType signatureTimeStamp = XAdESUtils.unmarshall(
					signatureTimeStampElement, XAdESTimeStampType.class);
			List<TimeStampToken> signatureTimeStampTokens = XAdESSignatureTimeStampValidation
					.validate(signatureTimeStamp, signatureElement);

			// XAdES-X

			// validate SigAndRefsTimeStamp
			Element sigAndRefsTimeStampElement = XAdESUtils
					.findUnsignedSignaturePropertyElement(
							qualifyingPropertiesElement, "SigAndRefsTimeStamp");
			if (null == sigAndRefsTimeStampElement) {
				LOG.error("No SigAndRefsTimeStamp present");
				throw new XAdESValidationException(
						"no xades:SigAndRefsTimeStamp present");
			}
			XAdESTimeStampType sigAndRefsTimeStamp = XAdESUtils.unmarshall(
					sigAndRefsTimeStampElement, XAdESTimeStampType.class);
			List<TimeStampToken> sigAndRefsTimeStampTokens = XAdESSigAndRefsTimeStampValidation
					.validate(sigAndRefsTimeStamp, signatureElement);

			// timestamp tokens trust validation
			LOG.debug("validate SignatureTimeStamp's trust...");
			ValidationDataType signatureTimeStampValidationData = XAdESUtils
					.findNextSibling(signatureTimeStampElement,
							XAdESUtils.XADES_141_NS_URI,
							"TimeStampValidationData", ValidationDataType.class);
			if (null != signatureTimeStampValidationData) {
				LOG.debug("xadesv141:TimeStampValidationData present for xades:SignatureTimeStamp");
				RevocationValuesType revocationValues = signatureTimeStampValidationData
						.getRevocationValues();
				List<X509CRL> crls = XAdESUtils.getCrls(revocationValues);
				List<OCSPResp> ocspResponses = XAdESUtils
						.getOCSPResponses(revocationValues);
				for (TimeStampToken signatureTimeStampToken : signatureTimeStampTokens) {
					this.documentContext.validate(signatureTimeStampToken,
							ocspResponses, crls);
				}
			} else {
				for (TimeStampToken signatureTimeStampToken : signatureTimeStampTokens) {
					this.documentContext.validate(signatureTimeStampToken);
				}
			}

			LOG.debug("validate SigAndRefsTimeStamp's trust...");
			ValidationDataType sigAndRefsTimeStampValidationData = XAdESUtils
					.findNextSibling(sigAndRefsTimeStampElement,
							XAdESUtils.XADES_141_NS_URI,
							"TimeStampValidationData", ValidationDataType.class);
			if (null != sigAndRefsTimeStampValidationData) {
				LOG.debug("xadesv141:TimeStampValidationData present for xades:SigAndRefsTimeStamp");
				RevocationValuesType revocationValues = sigAndRefsTimeStampValidationData
						.getRevocationValues();
				List<X509CRL> crls = XAdESUtils.getCrls(revocationValues);
				List<OCSPResp> ocspResponses = XAdESUtils
						.getOCSPResponses(revocationValues);
				for (TimeStampToken sigAndRefsTimeStampToken : sigAndRefsTimeStampTokens) {
					this.documentContext.validate(sigAndRefsTimeStampToken,
							ocspResponses, crls);
				}
			} else {
				for (TimeStampToken sigAndRefsTimeStampToken : sigAndRefsTimeStampTokens) {
					this.documentContext.validate(sigAndRefsTimeStampToken);
				}
			}

			// timestamp tokens time coherence verification
			long timestampMaxOffset = documentContext.getTimestampMaxOffset();
			LOG.debug("validate timestamp tokens time coherence...");
			for (TimeStampToken signatureTimeStampToken : signatureTimeStampTokens) {

				DateTime stsTokenGenTime = new DateTime(signatureTimeStampToken
						.getTimeStampInfo().getGenTime());

				if (stsTokenGenTime.plus(timestampMaxOffset).isBefore(
						signingTime)
						&& stsTokenGenTime.minus(timestampMaxOffset).isBefore(
								signingTime)) {
					throw new XAdESValidationException("SignatureTimeStamp ("
							+ signatureTimeStampToken.getTimeStampInfo()
									.getGenTime()
							+ ") generated before SigningTime (" + signingTime
							+ ")?!");
				}

				for (TimeStampToken sigAndRefsTimeStampToken : sigAndRefsTimeStampTokens) {

					DateTime sigAndRefsTokenGenTime = new DateTime(
							sigAndRefsTimeStampToken.getTimeStampInfo()
									.getGenTime());

					if (stsTokenGenTime.isAfter(sigAndRefsTokenGenTime
							.minus(timestampMaxOffset))
							&& stsTokenGenTime.isAfter(sigAndRefsTokenGenTime
									.plus(timestampMaxOffset))) {

						throw new XAdESValidationException(
								"SignatureTimeStamp ("
										+ signatureTimeStampToken
												.getTimeStampInfo()
												.getGenTime()
										+ ") generated after SigAndRefsTimeStamp ("
										+ sigAndRefsTimeStampToken
												.getTimeStampInfo()
												.getGenTime() + ") ?!");
					}
				}
			}

			// XAdES-C/XL

			/*
			 * Retrieve certificate chain and revocation data from XAdES-X-L
			 * extension for trust validation.
			 */
			RevocationValuesType revocationValues = XAdESUtils
					.findUnsignedSignatureProperty(qualifyingProperties,
							RevocationValuesType.class);
			List<X509CRL> crls = XAdESUtils.getCrls(revocationValues);
			List<OCSPResp> ocspResponses = XAdESUtils
					.getOCSPResponses(revocationValues);

			CertificateValuesType certificateValues = XAdESUtils
					.findUnsignedSignatureProperty(qualifyingProperties,
							CertificateValuesType.class);
			if (null == certificateValues) {
				LOG.error("no CertificateValuesType element found.");
				throw new XAdESValidationException(
						"no CertificateValuesType element found.");
			}
			List<X509Certificate> certificateChain = XAdESUtils
					.getCertificates(certificateValues);
			if (certificateChain.isEmpty()) {
				LOG.error("no certificate chain present in CertificateValuesType");
				throw new XAdESValidationException(
						"no cert chain in CertificateValuesType");
			}

			/*
			 * Check certificate chain is indeed contains the signing
			 * certificate.
			 */
			if (!Arrays.equals(signingCertificate.getEncoded(),
					certificateChain.get(0).getEncoded())) {
				// throw new XAdESValidationException(
				// "XAdES certificate chain does not include actual signing certificate");
				/*
				 * Not all XAdES implementations add the entire certificate
				 * chain via xades:CertificateValues.
				 */
				certificateChain.add(0, signingCertificate);
			}
			LOG.debug("XAdES certificate chain contains actual signing certificate");

			/*
			 * Perform trust validation via eID Trust Service
			 */
			this.documentContext.validate(certificateChain,
					signingTime.toDate(), ocspResponses, crls);

			/*
			 * Retrieve the possible eID identity signature extension data.
			 */
			String firstName = null;
			String name = null;
			String middleName = null;
			SignatureInfo.Gender gender = null;
			byte[] photo = null;

			IdentityType identity = XAdESUtils.findIdentity(nsElement,
					xmlSignature, signatureElement);
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
			return new SignatureInfo(signingCertificate, signingTime.toDate(),
					role, firstName, name, middleName, gender, photo);
		} catch (CertificateEncodingException e) {
			throw new XAdESValidationException(e);
		} catch (Exception e) {
			throw new XAdESValidationException(e);
		}
	}

	private void checkSigningCertificate(X509Certificate signingCertificate,
			SignedSignaturePropertiesType signedSignatureProperties)
			throws XAdESValidationException, CertificateEncodingException {
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
			throw new XAdESValidationException("message digest algo error: "
					+ e.getMessage(), e);
		}
		byte[] actualCertDigestValue = messageDigest.digest(signingCertificate
				.getEncoded());
		if (!Arrays.equals(actualCertDigestValue, certDigestValue)) {
			throw new XAdESValidationException(
					"XAdES signing certificate not corresponding with actual signing certificate");
		}
		LOG.debug("XAdES SigningCertificate OK");
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
				XAdESUtils.XADES_132_NS_URI);
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:xades141",
				XAdESUtils.XADES_141_NS_URI);
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:identity",
				IdentitySignatureFacet.NAMESPACE_URI);
		return nsElement;
	}
}
