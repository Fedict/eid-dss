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

import javax.ejb.EJB;
import javax.jws.WebService;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import be.fedict.eid.dss.model.SignatureVerificationService;
import be.fedict.eid.dss.model.exception.DocumentFormatException;
import be.fedict.eid.dss.model.exception.InvalidSignatureException;
import be.fedict.eid.dss.spi.SignatureInfo;
import be.fedict.eid.dss.ws.jaxb.dss.AnyType;
import be.fedict.eid.dss.ws.jaxb.dss.Base64Data;
import be.fedict.eid.dss.ws.jaxb.dss.DocumentType;
import be.fedict.eid.dss.ws.jaxb.dss.InputDocuments;
import be.fedict.eid.dss.ws.jaxb.dss.ObjectFactory;
import be.fedict.eid.dss.ws.jaxb.dss.ResponseBaseType;
import be.fedict.eid.dss.ws.jaxb.dss.Result;
import be.fedict.eid.dss.ws.jaxb.dss.VerifyRequest;
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
import be.fedict.eid.dss.ws.profile.vr.jaxb.xades.ClaimedRolesListType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.xmldsig.X509IssuerSerialType;

import com.sun.xml.ws.developer.UsesJAXBContext;

/**
 * Implementation of the DSS verification web service JAX-WS endpoint.
 * 
 * TODO: we can use this directly as servlet in web.xml. Before doing so we
 * should activate the Metro JBossWS.
 * 
 * @author Frank Cornelis
 * 
 */
@WebService(endpointInterface = "be.fedict.eid.dss.ws.DigitalSignatureServicePortType")
@ServiceConsumer
@UsesJAXBContext(DSSJAXBContextFactory.class)
public class DigitalSignatureServicePortImpl implements
		DigitalSignatureServicePortType {

	private static final Log LOG = LogFactory
			.getLog(DigitalSignatureServicePortImpl.class);

	@EJB
	private SignatureVerificationService signatureVerificationService;

	private final ObjectFactory dssObjectFactory;
	private final be.fedict.eid.dss.ws.profile.vr.jaxb.ObjectFactory vrObjectFactory;
	private final be.fedict.eid.dss.ws.profile.vr.jaxb.dss.ObjectFactory vrDssObjectFactory;
	private final be.fedict.eid.dss.ws.profile.vr.jaxb.xmldsig.ObjectFactory vrXmldsigObjectFactory;
	private final be.fedict.eid.dss.ws.profile.vr.jaxb.xades.ObjectFactory vrXadesObjectFactory;

	private final Marshaller vrMarshaller;
	private final DocumentBuilder documentBuilder;

	private final DatatypeFactory datatypeFactory;

	public DigitalSignatureServicePortImpl() {
		this.dssObjectFactory = new ObjectFactory();
		this.vrObjectFactory = new be.fedict.eid.dss.ws.profile.vr.jaxb.ObjectFactory();
		this.vrDssObjectFactory = new be.fedict.eid.dss.ws.profile.vr.jaxb.dss.ObjectFactory();
		this.vrXmldsigObjectFactory = new be.fedict.eid.dss.ws.profile.vr.jaxb.xmldsig.ObjectFactory();
		this.vrXadesObjectFactory = new be.fedict.eid.dss.ws.profile.vr.jaxb.xades.ObjectFactory();

		try {
			JAXBContext vrJAXBContext = JAXBContext
					.newInstance(be.fedict.eid.dss.ws.profile.vr.jaxb.ObjectFactory.class);
			this.vrMarshaller = vrJAXBContext.createMarshaller();
		} catch (JAXBException e) {
			throw new RuntimeException("JAXB error: " + e.getMessage(), e);
		}

		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		try {
			this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException("document builder error: "
					+ e.getMessage(), e);
		}

		try {
			this.datatypeFactory = DatatypeFactory.newInstance();
		} catch (DatatypeConfigurationException e) {
			throw new RuntimeException("datatype config error: "
					+ e.getMessage(), e);
		}
	}

	public ResponseBaseType verify(VerifyRequest verifyRequest) {
		LOG.debug("verify");

		/*
		 * Parse the request.
		 */
		String requestId = verifyRequest.getRequestID();
		LOG.debug("request Id: " + requestId);
		InputDocuments inputDocuments = verifyRequest.getInputDocuments();
		List<Object> documentObjects = inputDocuments
				.getDocumentOrTransformedDataOrDocumentHash();
		if (1 != documentObjects.size()) {
			LOG.error("can validate only one document");
			return createRequestorErrorResponse(requestId);
		}
		Object documentObject = documentObjects.get(0);
		if (false == documentObject instanceof DocumentType) {
			return createRequestorErrorResponse(requestId);
		}
		DocumentType document = (DocumentType) documentObject;
		Base64Data base64Data = document.getBase64Data();
		byte[] data;
		String mimeType;
		if (null != base64Data) {
			data = base64Data.getValue();
			mimeType = base64Data.getMimeType();
		} else {
			data = document.getBase64XML();
			mimeType = "text/xml";
		}
		if (null == data) {
			return createRequestorErrorResponse(requestId);
		}
		if (null == mimeType) {
			return createRequestorErrorResponse(requestId);
		}

		boolean returnVerificationReport = false;
		AnyType optionalInput = verifyRequest.getOptionalInputs();
		if (null != optionalInput) {
			List<Object> anyObjects = optionalInput.getAny();
			for (Object anyObject : anyObjects) {
				if (anyObject instanceof Element) {
					Element element = (Element) anyObject;
					if (DigitalSignatureServiceConstants.VR_NAMESPACE
							.equals(element.getNamespaceURI())
							&& "ReturnVerificationReport".equals(element
									.getLocalName())) {
						/*
						 * We could check for supported ReturnVerificationReport
						 * input, but then again, who cares?
						 */
						returnVerificationReport = true;
					}
				}
			}
		}

		/*
		 * Invoke the underlying DSS verification service.
		 */
		List<SignatureInfo> signatureInfos;
		try {
			signatureInfos = this.signatureVerificationService.verify(data,
					mimeType);
		} catch (DocumentFormatException e) {
			return createRequestorErrorResponse(
					requestId,
					DigitalSignatureServiceConstants.RESULT_MINOR_NOT_PARSEABLE_XML_DOCUMENT);
		} catch (InvalidSignatureException e) {
			return createRequestorErrorResponse(requestId);
		}

		/*
		 * Construct the DSS response.
		 */
		ResponseBaseType responseBase = this.dssObjectFactory
				.createResponseBaseType();
		responseBase.setRequestID(requestId);
		Result result = this.dssObjectFactory.createResult();
		result.setResultMajor(DigitalSignatureServiceConstants.RESULT_MAJOR_SUCCESS);
		AnyType optionalOutput = this.dssObjectFactory.createAnyType();
		if (signatureInfos.size() > 1) {
			result.setResultMinor(DigitalSignatureServiceConstants.RESULT_MINOR_VALID_MULTI_SIGNATURES);
		} else if (1 == signatureInfos.size()) {
			result.setResultMinor(DigitalSignatureServiceConstants.RESULT_MINOR_VALID_SIGNATURE);
		} else {
			result.setResultMinor(DigitalSignatureServiceConstants.RESULT_MINOR_INVALID_SIGNATURE);
		}

		if (returnVerificationReport) {
			LOG.debug("return verification report");
			VerificationReportType verificationReport = this.vrObjectFactory
					.createVerificationReportType();
			List<IndividualReportType> individualReports = verificationReport
					.getIndividualReport();
			for (SignatureInfo signatureInfo : signatureInfos) {
				X509Certificate signerCertificate = signatureInfo.getSigner();
				IndividualReportType individualReport = this.vrObjectFactory
						.createIndividualReportType();
				individualReports.add(individualReport);

				SignedObjectIdentifierType signedObjectIdentifier = this.vrObjectFactory
						.createSignedObjectIdentifierType();
				individualReport
						.setSignedObjectIdentifier(signedObjectIdentifier);
				SignedPropertiesType signedProperties = this.vrObjectFactory
						.createSignedPropertiesType();
				signedObjectIdentifier.setSignedProperties(signedProperties);
				SignedSignaturePropertiesType signedSignatureProperties = this.vrObjectFactory
						.createSignedSignaturePropertiesType();
				signedProperties
						.setSignedSignatureProperties(signedSignatureProperties);
				GregorianCalendar calendar = new GregorianCalendar();
				calendar.setTime(signatureInfo.getSigningTime());
				signedSignatureProperties.setSigningTime(this.datatypeFactory
						.newXMLGregorianCalendar(calendar));

				be.fedict.eid.dss.ws.profile.vr.jaxb.dss.Result individualResult = this.vrDssObjectFactory
						.createResult();
				individualReport.setResult(individualResult);
				individualResult
						.setResultMajor(DigitalSignatureServiceConstants.RESULT_MAJOR_SUCCESS);
				individualResult
						.setResultMinor(DigitalSignatureServiceConstants.RESULT_MINOR_VALID_SIGNATURE);

				be.fedict.eid.dss.ws.profile.vr.jaxb.dss.AnyType details = this.vrDssObjectFactory
						.createAnyType();
				individualReport.setDetails(details);

				DetailedSignatureReportType detailedSignatureReport = this.vrObjectFactory
						.createDetailedSignatureReportType();
				details.getAny()
						.add(this.vrObjectFactory
								.createDetailedSignatureReport(detailedSignatureReport));
				VerificationResultType formatOKVerificationResult = this.vrObjectFactory
						.createVerificationResultType();
				formatOKVerificationResult
						.setResultMajor(DigitalSignatureServiceConstants.VR_RESULT_MAJOR_VALID);
				detailedSignatureReport.setFormatOK(formatOKVerificationResult);

				SignatureValidityType signatureOkSignatureValidity = this.vrObjectFactory
						.createSignatureValidityType();
				detailedSignatureReport
						.setSignatureOK(signatureOkSignatureValidity);
				VerificationResultType sigMathOkVerificationResult = this.vrObjectFactory
						.createVerificationResultType();
				signatureOkSignatureValidity
						.setSigMathOK(sigMathOkVerificationResult);
				sigMathOkVerificationResult
						.setResultMajor(DigitalSignatureServiceConstants.VR_RESULT_MAJOR_VALID);

				if (null != signatureInfo.getRole()) {
					PropertiesType properties = this.vrObjectFactory
							.createPropertiesType();
					detailedSignatureReport.setProperties(properties);
					SignedPropertiesType vrSignedProperties = this.vrObjectFactory
							.createSignedPropertiesType();
					properties.setSignedProperties(vrSignedProperties);
					SignedSignaturePropertiesType vrSignedSignatureProperties = this.vrObjectFactory
							.createSignedSignaturePropertiesType();
					vrSignedProperties
							.setSignedSignatureProperties(vrSignedSignatureProperties);
					vrSignedSignatureProperties
							.setSigningTime(this.datatypeFactory
									.newXMLGregorianCalendar(calendar));
					SignerRoleType signerRole = this.vrObjectFactory
							.createSignerRoleType();
					vrSignedSignatureProperties.setSignerRole(signerRole);
					ClaimedRolesListType claimedRolesList = this.vrXadesObjectFactory
							.createClaimedRolesListType();
					signerRole.setClaimedRoles(claimedRolesList);
					be.fedict.eid.dss.ws.profile.vr.jaxb.xades.AnyType claimedRoleAny = this.vrXadesObjectFactory
							.createAnyType();
					claimedRolesList.getClaimedRole().add(claimedRoleAny);
					claimedRoleAny.getContent().add(signatureInfo.getRole());
				}

				CertificatePathValidityType certificatePathValidity = this.vrObjectFactory
						.createCertificatePathValidityType();
				detailedSignatureReport
						.setCertificatePathValidity(certificatePathValidity);

				VerificationResultType certPathVerificationResult = this.vrObjectFactory
						.createVerificationResultType();
				certPathVerificationResult
						.setResultMajor(DigitalSignatureServiceConstants.VR_RESULT_MAJOR_VALID);
				certificatePathValidity
						.setPathValiditySummary(certPathVerificationResult);

				X509IssuerSerialType certificateIdentifier = this.vrXmldsigObjectFactory
						.createX509IssuerSerialType();
				certificatePathValidity
						.setCertificateIdentifier(certificateIdentifier);
				certificateIdentifier.setX509IssuerName(signerCertificate
						.getIssuerX500Principal().toString());
				certificateIdentifier.setX509SerialNumber(signerCertificate
						.getSerialNumber());

				CertificatePathValidityVerificationDetailType certificatePathValidityVerificationDetail = this.vrObjectFactory
						.createCertificatePathValidityVerificationDetailType();
				certificatePathValidity
						.setPathValidityDetail(certificatePathValidityVerificationDetail);
				CertificateValidityType certificateValidity = this.vrObjectFactory
						.createCertificateValidityType();
				certificatePathValidityVerificationDetail
						.getCertificateValidity().add(certificateValidity);
				certificateValidity
						.setCertificateIdentifier(certificateIdentifier);
				certificateValidity.setSubject(signerCertificate
						.getSubjectX500Principal().toString());

				VerificationResultType chainingOkVerificationResult = this.vrObjectFactory
						.createVerificationResultType();
				certificateValidity.setChainingOK(chainingOkVerificationResult);
				chainingOkVerificationResult
						.setResultMajor(DigitalSignatureServiceConstants.VR_RESULT_MAJOR_VALID);

				VerificationResultType validityPeriodOkVerificationResult = this.vrObjectFactory
						.createVerificationResultType();
				certificateValidity
						.setValidityPeriodOK(validityPeriodOkVerificationResult);
				validityPeriodOkVerificationResult
						.setResultMajor(DigitalSignatureServiceConstants.VR_RESULT_MAJOR_VALID);

				VerificationResultType extensionsOkVerificationResult = this.vrObjectFactory
						.createVerificationResultType();
				certificateValidity
						.setExtensionsOK(extensionsOkVerificationResult);
				extensionsOkVerificationResult
						.setResultMajor(DigitalSignatureServiceConstants.VR_RESULT_MAJOR_VALID);

				try {
					certificateValidity.setCertificateValue(signerCertificate
							.getEncoded());
				} catch (CertificateEncodingException e) {
					throw new RuntimeException("X509 encoding error: "
							+ e.getMessage(), e);
				}

				certificateValidity
						.setSignatureOK(signatureOkSignatureValidity);

				CertificateStatusType certificateStatus = this.vrObjectFactory
						.createCertificateStatusType();
				certificateValidity.setCertificateStatus(certificateStatus);
				VerificationResultType certStatusOkVerificationResult = this.vrObjectFactory
						.createVerificationResultType();
				certificateStatus
						.setCertStatusOK(certStatusOkVerificationResult);
				certStatusOkVerificationResult
						.setResultMajor(DigitalSignatureServiceConstants.VR_RESULT_MAJOR_VALID);
			}

			Document newDocument = this.documentBuilder.newDocument();
			Element newElement = (Element) newDocument.createElement("newNode");
			try {
				this.vrMarshaller.marshal(this.vrObjectFactory
						.createVerificationReport(verificationReport),
						newElement);
			} catch (JAXBException e) {
				throw new RuntimeException("JAXB error: " + e.getMessage(), e);
			}
			Element verificationReportElement = (Element) newElement
					.getFirstChild();
			optionalOutput.getAny().add(verificationReportElement);
		}

		if (false == optionalOutput.getAny().isEmpty()) {
			responseBase.setOptionalOutputs(optionalOutput);
		}

		responseBase.setResult(result);
		return responseBase;
	}

	private ResponseBaseType createRequestorErrorResponse(String requestId) {
		return createRequestorErrorResponse(requestId, null);
	}

	private ResponseBaseType createRequestorErrorResponse(String requestId,
			String resultMinor) {
		ResponseBaseType responseBase = this.dssObjectFactory
				.createResponseBaseType();
		responseBase.setRequestID(requestId);
		Result result = this.dssObjectFactory.createResult();
		result.setResultMajor(DigitalSignatureServiceConstants.RESULT_MAJOR_REQUESTER_ERROR);
		if (null != resultMinor) {
			result.setResultMinor(resultMinor);
		}
		responseBase.setResult(result);
		return responseBase;
	}
}
