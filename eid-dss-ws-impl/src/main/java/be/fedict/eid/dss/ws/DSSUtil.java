/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009-2010 FedICT.
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

package be.fedict.eid.dss.ws;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.GregorianCalendar;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import be.fedict.eid.dss.spi.SignatureInfo;
import be.fedict.eid.dss.ws.jaxb.dss.AnyType;
import be.fedict.eid.dss.ws.jaxb.dss.InternationalStringType;
import be.fedict.eid.dss.ws.jaxb.dss.ObjectFactory;
import be.fedict.eid.dss.ws.jaxb.dss.ResponseBaseType;
import be.fedict.eid.dss.ws.jaxb.dss.Result;
import be.fedict.eid.dss.ws.jaxb.dss.SignResponse;
import be.fedict.eid.dss.ws.profile.artifact.jaxb.ReturnStoredDocument;
import be.fedict.eid.dss.ws.profile.artifact.jaxb.StorageInfo;
import be.fedict.eid.dss.ws.profile.originaldocument.jaxb.OriginalDocumentType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.CertificatePathValidityType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.CertificatePathValidityVerificationDetailType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.CertificateStatusType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.CertificateValidityType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.DetailedSignatureReportType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.IndividualReportType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.PropertiesType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.SignatureValidityType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.SignedObjectIdentifierType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.SignedPropertiesType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.SignedSignaturePropertiesType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.SignerRoleType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.VerificationReportType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.VerificationResultType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.dss.Base64Data;
import be.fedict.eid.dss.ws.profile.vr.jaxb.dss.DocumentType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.dss.InputDocuments;
import be.fedict.eid.dss.ws.profile.vr.jaxb.xades.ClaimedRolesListType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.xmldsig.X509IssuerSerialType;

/**
 * DSS WS Utility class
 */
public abstract class DSSUtil {

	private static final Log LOG = LogFactory.getLog(DSSUtil.class);

	private static final be.fedict.eid.dss.ws.profile.vr.jaxb.ObjectFactory vrObjectFactory;
	private static final be.fedict.eid.dss.ws.profile.vr.jaxb.dss.ObjectFactory vrDssObjectFactory;
	private static final be.fedict.eid.dss.ws.profile.vr.jaxb.xmldsig.ObjectFactory vrXmldsigObjectFactory;
	private static final be.fedict.eid.dss.ws.profile.vr.jaxb.xades.ObjectFactory vrXadesObjectFactory;

	private static final Marshaller vrMarshaller;
	private static final Marshaller artifactMarshaller;
	private static final Unmarshaller artifactUnmarshaller;
	private static final DocumentBuilder documentBuilder;

	private static final DatatypeFactory datatypeFactory;
	private static final be.fedict.eid.dss.ws.jaxb.dss.ObjectFactory dssObjectFactory;
	private static final be.fedict.eid.dss.ws.profile.artifact.jaxb.ObjectFactory artifactObjectFactory;

	private static final Unmarshaller originalDocumentUnmarshaller;

	static {
		vrObjectFactory = new be.fedict.eid.dss.ws.profile.vr.jaxb.ObjectFactory();
		vrDssObjectFactory = new be.fedict.eid.dss.ws.profile.vr.jaxb.dss.ObjectFactory();
		vrXmldsigObjectFactory = new be.fedict.eid.dss.ws.profile.vr.jaxb.xmldsig.ObjectFactory();
		vrXadesObjectFactory = new be.fedict.eid.dss.ws.profile.vr.jaxb.xades.ObjectFactory();
		dssObjectFactory = new ObjectFactory();
		artifactObjectFactory = new be.fedict.eid.dss.ws.profile.artifact.jaxb.ObjectFactory();

		try {
			JAXBContext vrJAXBContext = JAXBContext
					.newInstance(be.fedict.eid.dss.ws.profile.vr.jaxb.ObjectFactory.class);
			vrMarshaller = vrJAXBContext.createMarshaller();

			JAXBContext artifactJAXBContext = JAXBContext
					.newInstance(be.fedict.eid.dss.ws.profile.artifact.jaxb.ObjectFactory.class);
			artifactMarshaller = artifactJAXBContext.createMarshaller();
			artifactUnmarshaller = artifactJAXBContext.createUnmarshaller();

			JAXBContext originalDocumentJAXBContext = JAXBContext
					.newInstance(be.fedict.eid.dss.ws.profile.originaldocument.jaxb.ObjectFactory.class);
			originalDocumentUnmarshaller = originalDocumentJAXBContext
					.createUnmarshaller();
		} catch (JAXBException e) {
			throw new RuntimeException("JAXB error: " + e.getMessage(), e);
		}

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		try {
			documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("document builder error: "
					+ e.getMessage(), e);
		}

		try {
			datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException("datatype config error: "
					+ e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	public static ReturnStoredDocument getReturnStoredDocument(
			Element returnStoredDocumentElement) throws JAXBException {

		JAXBElement<ReturnStoredDocument> jaxbElement = (JAXBElement<ReturnStoredDocument>) artifactUnmarshaller
				.unmarshal(returnStoredDocumentElement);
		return jaxbElement.getValue();
	}

	/**
	 * Adds a DSS Verification Report to specified optional output element from
	 * the specified list of {@link SignatureInfo}'s
	 * 
	 * @param optionalOutput
	 *            optional output to add verification report to
	 * @param signatureInfos
	 *            signature infos to use in verification report.
	 */
	public static void addVerificationReport(AnyType optionalOutput,
			List<SignatureInfo> signatureInfos) {

		LOG.debug("return verification report");
		VerificationReportType verificationReport = vrObjectFactory
				.createVerificationReportType();
		List<IndividualReportType> individualReports = verificationReport
				.getIndividualReport();
		for (SignatureInfo signatureInfo : signatureInfos) {
			X509Certificate signerCertificate = signatureInfo.getSigner();
			IndividualReportType individualReport = vrObjectFactory
					.createIndividualReportType();
			individualReports.add(individualReport);

			SignedObjectIdentifierType signedObjectIdentifier = vrObjectFactory
					.createSignedObjectIdentifierType();
			individualReport.setSignedObjectIdentifier(signedObjectIdentifier);
			SignedPropertiesType signedProperties = vrObjectFactory
					.createSignedPropertiesType();
			signedObjectIdentifier.setSignedProperties(signedProperties);
			SignedSignaturePropertiesType signedSignatureProperties = vrObjectFactory
					.createSignedSignaturePropertiesType();
			signedProperties
					.setSignedSignatureProperties(signedSignatureProperties);
			GregorianCalendar calendar = new GregorianCalendar();
			calendar.setTime(signatureInfo.getSigningTime());
			signedSignatureProperties.setSigningTime(datatypeFactory
					.newXMLGregorianCalendar(calendar));

			be.fedict.eid.dss.ws.profile.vr.jaxb.dss.Result individualResult = vrDssObjectFactory
					.createResult();
			individualReport.setResult(individualResult);
			individualResult.setResultMajor(DSSConstants.RESULT_MAJOR_SUCCESS);
			individualResult
					.setResultMinor(DSSConstants.RESULT_MINOR_VALID_SIGNATURE);

			be.fedict.eid.dss.ws.profile.vr.jaxb.dss.AnyType details = vrDssObjectFactory
					.createAnyType();
			individualReport.setDetails(details);

			DetailedSignatureReportType detailedSignatureReport = vrObjectFactory
					.createDetailedSignatureReportType();
			details.getAny()
					.add(vrObjectFactory
							.createDetailedSignatureReport(detailedSignatureReport));
			VerificationResultType formatOKVerificationResult = vrObjectFactory
					.createVerificationResultType();
			formatOKVerificationResult
					.setResultMajor(DSSConstants.VR_RESULT_MAJOR_VALID);
			detailedSignatureReport.setFormatOK(formatOKVerificationResult);

			SignatureValidityType signatureOkSignatureValidity = vrObjectFactory
					.createSignatureValidityType();
			detailedSignatureReport
					.setSignatureOK(signatureOkSignatureValidity);
			VerificationResultType sigMathOkVerificationResult = vrObjectFactory
					.createVerificationResultType();
			signatureOkSignatureValidity
					.setSigMathOK(sigMathOkVerificationResult);
			sigMathOkVerificationResult
					.setResultMajor(DSSConstants.VR_RESULT_MAJOR_VALID);

			if (null != signatureInfo.getRole()) {
				PropertiesType properties = vrObjectFactory
						.createPropertiesType();
				detailedSignatureReport.setProperties(properties);
				SignedPropertiesType vrSignedProperties = vrObjectFactory
						.createSignedPropertiesType();
				properties.setSignedProperties(vrSignedProperties);
				SignedSignaturePropertiesType vrSignedSignatureProperties = vrObjectFactory
						.createSignedSignaturePropertiesType();
				vrSignedProperties
						.setSignedSignatureProperties(vrSignedSignatureProperties);
				vrSignedSignatureProperties.setSigningTime(datatypeFactory
						.newXMLGregorianCalendar(calendar));
				SignerRoleType signerRole = vrObjectFactory
						.createSignerRoleType();
				vrSignedSignatureProperties.setSignerRole(signerRole);
				ClaimedRolesListType claimedRolesList = vrXadesObjectFactory
						.createClaimedRolesListType();
				signerRole.setClaimedRoles(claimedRolesList);
				be.fedict.eid.dss.ws.profile.vr.jaxb.xades.AnyType claimedRoleAny = vrXadesObjectFactory
						.createAnyType();
				claimedRolesList.getClaimedRole().add(claimedRoleAny);
				claimedRoleAny.getContent().add(signatureInfo.getRole());
			}

			CertificatePathValidityType certificatePathValidity = vrObjectFactory
					.createCertificatePathValidityType();
			detailedSignatureReport
					.setCertificatePathValidity(certificatePathValidity);

			VerificationResultType certPathVerificationResult = vrObjectFactory
					.createVerificationResultType();
			certPathVerificationResult
					.setResultMajor(DSSConstants.VR_RESULT_MAJOR_VALID);
			certificatePathValidity
					.setPathValiditySummary(certPathVerificationResult);

			X509IssuerSerialType certificateIdentifier = vrXmldsigObjectFactory
					.createX509IssuerSerialType();
			certificatePathValidity
					.setCertificateIdentifier(certificateIdentifier);
			certificateIdentifier.setX509IssuerName(signerCertificate
					.getIssuerX500Principal().toString());
			certificateIdentifier.setX509SerialNumber(signerCertificate
					.getSerialNumber());

			CertificatePathValidityVerificationDetailType certificatePathValidityVerificationDetail = vrObjectFactory
					.createCertificatePathValidityVerificationDetailType();
			certificatePathValidity
					.setPathValidityDetail(certificatePathValidityVerificationDetail);
			CertificateValidityType certificateValidity = vrObjectFactory
					.createCertificateValidityType();
			certificatePathValidityVerificationDetail.getCertificateValidity()
					.add(certificateValidity);
			certificateValidity.setCertificateIdentifier(certificateIdentifier);
			certificateValidity.setSubject(signerCertificate
					.getSubjectX500Principal().toString());

			VerificationResultType chainingOkVerificationResult = vrObjectFactory
					.createVerificationResultType();
			certificateValidity.setChainingOK(chainingOkVerificationResult);
			chainingOkVerificationResult
					.setResultMajor(DSSConstants.VR_RESULT_MAJOR_VALID);

			VerificationResultType validityPeriodOkVerificationResult = vrObjectFactory
					.createVerificationResultType();
			certificateValidity
					.setValidityPeriodOK(validityPeriodOkVerificationResult);
			validityPeriodOkVerificationResult
					.setResultMajor(DSSConstants.VR_RESULT_MAJOR_VALID);

			VerificationResultType extensionsOkVerificationResult = vrObjectFactory
					.createVerificationResultType();
			certificateValidity.setExtensionsOK(extensionsOkVerificationResult);
			extensionsOkVerificationResult
					.setResultMajor(DSSConstants.VR_RESULT_MAJOR_VALID);

			try {
				certificateValidity.setCertificateValue(signerCertificate
						.getEncoded());
			} catch (CertificateEncodingException e) {
				throw new RuntimeException("X509 encoding error: "
						+ e.getMessage(), e);
			}

			certificateValidity.setSignatureOK(signatureOkSignatureValidity);

			CertificateStatusType certificateStatus = vrObjectFactory
					.createCertificateStatusType();
			certificateValidity.setCertificateStatus(certificateStatus);
			VerificationResultType certStatusOkVerificationResult = vrObjectFactory
					.createVerificationResultType();
			certificateStatus.setCertStatusOK(certStatusOkVerificationResult);
			certStatusOkVerificationResult
					.setResultMajor(DSSConstants.VR_RESULT_MAJOR_VALID);
		}

		Document newDocument = documentBuilder.newDocument();
		Element newElement = newDocument.createElement("newNode");
		try {
			vrMarshaller.marshal(vrObjectFactory
					.createVerificationReport(verificationReport), newElement);
		} catch (JAXBException e) {
			throw new RuntimeException("JAXB error: " + e.getMessage(), e);
		}
		Element verificationReportElement = (Element) newElement
				.getFirstChild();
		optionalOutput.getAny().add(verificationReportElement);
	}

	public static Element getStorageInfoElement(StorageInfo storageInfo) {

		Document newDocument = documentBuilder.newDocument();
		Element newElement = newDocument.createElement("newNode");
		try {
			artifactMarshaller.marshal(
					artifactObjectFactory.createStorageInfo(storageInfo),
					newElement);
		} catch (JAXBException e) {
			throw new RuntimeException("JAXB error: " + e.getMessage(), e);
		}
		return (Element) newElement.getFirstChild();
	}

	public static SignResponse createRequestorSignErrorResponse(
			String requestId, String resultMinor, String message) {

		LOG.error("Error: resultMinor=" + resultMinor + " message=" + message);

		SignResponse signResponse = dssObjectFactory.createSignResponse();
		signResponse.setRequestID(requestId);
		signResponse
				.setResult(getResult(DSSConstants.RESULT_MAJOR_REQUESTER_ERROR,
						resultMinor, message));
		return signResponse;
	}

	public static ResponseBaseType createRequestorErrorResponse(String requestId) {
		return createRequestorErrorResponse(requestId, null);
	}

	public static ResponseBaseType createRequestorErrorResponse(
			String requestId, String resultMinor) {

		return createRequestorErrorResponse(requestId, resultMinor, null);
	}

	public static ResponseBaseType createRequestorErrorResponse(
			String requestId, String resultMinor, String message) {

		LOG.error("Error: resultMinor=" + resultMinor + " message=" + message);

		ResponseBaseType responseBase = dssObjectFactory
				.createResponseBaseType();
		responseBase.setRequestID(requestId);
		responseBase
				.setResult(getResult(DSSConstants.RESULT_MAJOR_REQUESTER_ERROR,
						resultMinor, message));
		return responseBase;
	}

	private static Result getResult(String resultMajor, String resultMinor,
			String message) {

		Result result = dssObjectFactory.createResult();
		result.setResultMajor(resultMajor);
		if (null != resultMinor) {
			result.setResultMinor(resultMinor);
		}
		if (null != message) {
			InternationalStringType resultMessage = dssObjectFactory
					.createInternationalStringType();
			resultMessage.setLang("en");
			resultMessage.setValue(message);
			result.setResultMessage(resultMessage);
		}
		return result;
	}

	public static XMLGregorianCalendar toXML(DateTime dateTime) {

		GregorianCalendar calendar = new GregorianCalendar();
		calendar.setTimeInMillis(dateTime.getMillis());
		return datatypeFactory.newXMLGregorianCalendar(calendar);
	}

	public static byte[] getOriginalDocument(Element element)
			throws JAXBException {
		JAXBElement<OriginalDocumentType> originalDocumentElement = (JAXBElement<OriginalDocumentType>) originalDocumentUnmarshaller
				.unmarshal(element);
		OriginalDocumentType originalDocument = originalDocumentElement
				.getValue();
		InputDocuments inputDocuments = originalDocument.getInputDocuments();
		List<Object> documentObjects = inputDocuments
				.getDocumentOrTransformedDataOrDocumentHash();
		for (Object documentObject : documentObjects) {
			if (!(documentObject instanceof DocumentType)) {
				continue;
			}
			DocumentType document = (DocumentType) documentObject;
			Base64Data base64Data = document.getBase64Data();
			byte[] data;
			if (null != base64Data) {
				data = base64Data.getValue();
			} else {
				data = document.getBase64XML();
			}
			if (null != data) {
				return data;
			}
		}
		return null;
	}
}
