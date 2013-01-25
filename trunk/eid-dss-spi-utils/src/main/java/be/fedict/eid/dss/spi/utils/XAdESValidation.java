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

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

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
import org.w3c.dom.NodeList;

import be.fedict.eid.applet.service.signer.facets.IdentitySignatureFacet;
import be.fedict.eid.applet.service.signer.jaxb.identity.IdentityType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.AnyType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CertificateValuesType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.ClaimedRolesListType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CompleteCertificateRefsType;
import be.fedict.eid.applet.service.signer.jaxb.xades132.CompleteRevocationRefsType;
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

	public void prepareDocument(Element signatureElement) {
		prepareDocumentXades(signatureElement);
		prepareDocumentIdentity(signatureElement);
	}

	private void prepareDocumentXades(Element signatureElement) {
		NodeList nodeList = signatureElement.getElementsByTagNameNS(
				"http://uri.etsi.org/01903/v1.3.2#", "SignedProperties");
		if (1 == nodeList.getLength()) {
			Element signedPropertiesElement = (Element) nodeList.item(0);
			signedPropertiesElement.setIdAttribute("Id", true);
		}
	}

	private void prepareDocumentIdentity(Element signatureElement) {
		NodeList nodeList = signatureElement.getElementsByTagNameNS(
				"be:fedict:eid:dss:stylesheet:1.0", "StyleSheet");
		if (1 == nodeList.getLength()) {
			Element styleSheetElement = (Element) nodeList.item(0);
			styleSheetElement.setIdAttribute("Id", true);
		}
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
			if (false == qualifyingProperties.getTarget().equals(
					"#" + xmlSignature.getId())) {
				throw new XAdESValidationException(
						"xades:QualifyingProperties/@Target incorrect");
			}

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
			XAdESUtils.checkSigningCertificate(signingCertificate,
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

			// validate first SignatureTimeStamp
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
					.verify(signatureTimeStamp, signatureElement);

			// XAdES-X

			// validate first SigAndRefsTimeStamp
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
					.verify(sigAndRefsTimeStamp, signatureElement);

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
			long timestampMaxOffset = this.documentContext
					.getTimestampMaxOffset();
			LOG.debug("validate timestamp tokens time coherence...");
			for (TimeStampToken signatureTimeStampToken : signatureTimeStampTokens) {
				DateTime stsTokenGenTime = new DateTime(signatureTimeStampToken
						.getTimeStampInfo().getGenTime());
				try {
					XAdESUtils.checkCloseEnough(signingTime, stsTokenGenTime,
							timestampMaxOffset);
				} catch (XAdESValidationException e) {
					throw new XAdESValidationException(
							"SignatureTimeStamp too far from SigningTime", e);
				}

				for (TimeStampToken sigAndRefsTimeStampToken : sigAndRefsTimeStampTokens) {
					DateTime sigAndRefsTokenGenTime = new DateTime(
							sigAndRefsTimeStampToken.getTimeStampInfo()
									.getGenTime());
					if (sigAndRefsTokenGenTime.isBefore(stsTokenGenTime)) {
						throw new XAdESValidationException(
								"SigAndRefsTimeStamp before SignatureTimeStamp");
					}
				}
			}

			long maxGracePeriod = this.documentContext.getMaxGracePeriod();
			for (TimeStampToken sigAndRefsTimeStampToken : sigAndRefsTimeStampTokens) {
				DateTime sigAndRefsTokenGenTime = new DateTime(
						sigAndRefsTimeStampToken.getTimeStampInfo()
								.getGenTime());
				try {
					XAdESUtils.checkCloseEnough(signingTime,
							sigAndRefsTokenGenTime,
							maxGracePeriod * 1000 * 60 * 60);
				} catch (XAdESValidationException e) {
					throw new XAdESValidationException(
							"SigAndRefsTimeStamp too far from SigningTime", e);
				}
			}

			// XAdES-X-L

			/*
			 * Retrieve certificate chain and revocation data from XAdES-X-L
			 * extension for trust validation.
			 */
			RevocationValuesType revocationValues = XAdESUtils
					.findUnsignedSignatureProperty(qualifyingProperties,
							RevocationValuesType.class, "RevocationValues");
			List<X509CRL> crls = XAdESUtils.getCrls(revocationValues);
			List<OCSPResp> ocspResponses = XAdESUtils
					.getOCSPResponses(revocationValues);

			CertificateValuesType certificateValues = XAdESUtils
					.findUnsignedSignatureProperty(qualifyingProperties,
							CertificateValuesType.class, "CertificateValues");
			if (null == certificateValues) {
				throw new XAdESValidationException(
						"no CertificateValues element found.");
			}
			List<X509Certificate> certificateChain = XAdESUtils
					.getCertificates(certificateValues);
			if (certificateChain.isEmpty()) {
				throw new XAdESValidationException(
						"no cert chain in CertificateValues");
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

			// XAdES-C
			CompleteCertificateRefsType completeCertificateRefs = XAdESUtils
					.findUnsignedSignatureProperty(qualifyingProperties,
							CompleteCertificateRefsType.class,
							"CompleteCertificateRefs");
			if (null == completeCertificateRefs) {
				throw new XAdESValidationException(
						"missing CompleteCertificateRefs");
			}
			CompleteRevocationRefsType completeRevocationRefs = XAdESUtils
					.findUnsignedSignatureProperty(qualifyingProperties,
							CompleteRevocationRefsType.class,
							"CompleteRevocationRefs");
			if (null == completeRevocationRefs) {
				throw new XAdESValidationException(
						"missing CompleteRevocationRefs");
			}
			for (OCSPResp ocspResp : ocspResponses) {
				XAdESUtils.checkReference(ocspResp, completeRevocationRefs);
			}
			for (X509CRL crl : crls) {
				XAdESUtils.checkReference(crl, completeRevocationRefs);
			}
			Iterator<X509Certificate> certIterator = certificateChain
					.iterator();
			certIterator.next(); // digestion of SigningCertificate already
									// checked
			while (certIterator.hasNext()) {
				X509Certificate certificate = certIterator.next();
				XAdESUtils.checkReference(certificate, completeCertificateRefs);
			}

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
