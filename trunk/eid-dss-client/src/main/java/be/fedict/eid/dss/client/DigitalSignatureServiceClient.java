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

package be.fedict.eid.dss.client;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import be.fedict.eid.dss.ws.DigitalSignatureService;
import be.fedict.eid.dss.ws.DigitalSignatureServiceConstants;
import be.fedict.eid.dss.ws.DigitalSignatureServiceFactory;
import be.fedict.eid.dss.ws.DigitalSignatureServicePortType;
import be.fedict.eid.dss.ws.jaxb.dss.AnyType;
import be.fedict.eid.dss.ws.jaxb.dss.Base64Data;
import be.fedict.eid.dss.ws.jaxb.dss.DocumentType;
import be.fedict.eid.dss.ws.jaxb.dss.InputDocuments;
import be.fedict.eid.dss.ws.jaxb.dss.ObjectFactory;
import be.fedict.eid.dss.ws.jaxb.dss.ResponseBaseType;
import be.fedict.eid.dss.ws.jaxb.dss.Result;
import be.fedict.eid.dss.ws.jaxb.dss.VerifyRequest;
import be.fedict.eid.dss.ws.profile.vr.jaxb.CertificateValidityType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.IndividualReportType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.ReturnVerificationReport;
import be.fedict.eid.dss.ws.profile.vr.jaxb.SignedObjectIdentifierType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.VerificationReportType;

/**
 * Client for the OASIS DSS verification web service.
 * 
 * @author Frank Cornelis
 * 
 */
public class DigitalSignatureServiceClient {

	public static final String DEFAULT_ENDPOINT_ADDRESS = "http://localhost:8080/eid-dss-ws/dss";

	private static final Log LOG = LogFactory
			.getLog(DigitalSignatureServiceClient.class);

	private final String endpointAddress;

	private final ObjectFactory dssObjectFactory;

	private final be.fedict.eid.dss.ws.profile.vr.jaxb.ObjectFactory vrObjectFactory;

	private final Marshaller vrMarshaller;
	private final Unmarshaller vrUnmarshaller;

	private final DocumentBuilder documentBuilder;

	private final CertificateFactory certificateFactory;

	/**
	 * Main constructor.
	 * 
	 * @param endpointAddress
	 *            the DSS web service endpoint address.
	 */
	public DigitalSignatureServiceClient(String endpointAddress) {
		this.endpointAddress = endpointAddress;
		this.dssObjectFactory = new ObjectFactory();
		this.vrObjectFactory = new be.fedict.eid.dss.ws.profile.vr.jaxb.ObjectFactory();
		try {
			JAXBContext vrJAXBContext = JAXBContext
					.newInstance(be.fedict.eid.dss.ws.profile.vr.jaxb.ObjectFactory.class);
			this.vrMarshaller = vrJAXBContext.createMarshaller();
			this.vrUnmarshaller = vrJAXBContext.createUnmarshaller();
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
			this.certificateFactory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			throw new RuntimeException("X509 factory error: " + e.getMessage(),
					e);
		}
	}

	/**
	 * Default constructor.
	 */
	public DigitalSignatureServiceClient() {
		this(DEFAULT_ENDPOINT_ADDRESS);
	}

	/**
	 * Verifies whether the given document has been signed or not.
	 * 
	 * @param signedDocument
	 * @param mimeType
	 *            optional mime-type, default is "text/xml".
	 * @return <code>true</code> is the document has been signed,
	 *         <code>false</code> otherwise.
	 * @throws NotParseableXMLDocumentException
	 */
	public boolean verify(byte[] signedDocument, String mimeType)
			throws NotParseableXMLDocumentException {
		ResponseBaseType responseBase = doVerification(signedDocument,
				mimeType, false, false);

		Result result = responseBase.getResult();
		String resultMajor = result.getResultMajor();
		LOG.debug("result major: " + resultMajor);
		String resultMinor = result.getResultMinor();
		if (false == DigitalSignatureServiceConstants.RESULT_MAJOR_SUCCESS
				.equals(resultMajor)) {
			LOG.warn("result minor: " + resultMinor);
			if (null != resultMinor
					&& DigitalSignatureServiceConstants.RESULT_MINOR_NOT_PARSEABLE_XML_DOCUMENT
							.equals(resultMinor)) {
				throw new NotParseableXMLDocumentException();
			}
			throw new RuntimeException("unsuccessful result: " + resultMajor);
		}
		if (null == resultMinor) {
			throw new RuntimeException("missing ResultMinor");
		}
		if (DigitalSignatureServiceConstants.RESULT_MINOR_VALID_SIGNATURE
				.equals(resultMinor)) {
			return true;
		}
		if (DigitalSignatureServiceConstants.RESULT_MINOR_VALID_MULTI_SIGNATURES
				.equals(resultMinor)) {
			return true;
		}
		return false;
	}

	/**
	 * Verifies whether the given document has been signed and reports back on
	 * the signing parties.
	 * 
	 * @param signedDocument
	 * @return a list of signature information objects detailing on the signing
	 *         parties.
	 * @throws NotParseableXMLDocumentException
	 */
	public List<SignatureInfo> verifyWithSigners(byte[] signedDocument,
			String mimeType) throws NotParseableXMLDocumentException {
		ResponseBaseType responseBase = doVerification(signedDocument,
				mimeType, false, true);

		Result result = responseBase.getResult();
		String resultMajor = result.getResultMajor();
		LOG.debug("result major: " + resultMajor);
		if (false == DigitalSignatureServiceConstants.RESULT_MAJOR_SUCCESS
				.equals(resultMajor)) {
			String resultMinor = result.getResultMinor();
			if (null != resultMinor
					&& DigitalSignatureServiceConstants.RESULT_MINOR_NOT_PARSEABLE_XML_DOCUMENT
							.equals(resultMinor)) {
				throw new NotParseableXMLDocumentException();
			}
			throw new RuntimeException("unsuccessful result: " + resultMajor);
		}

		List<SignatureInfo> signers = new LinkedList<SignatureInfo>();
		AnyType optionalOutputs = responseBase.getOptionalOutputs();
		if (null == optionalOutputs) {
			return signers;
		}
		List<Object> optionalOutputContent = optionalOutputs.getAny();
		for (Object optionalOutput : optionalOutputContent) {
			if (optionalOutput instanceof Element) {
				Element optionalOutputElement = (Element) optionalOutput;
				if (DigitalSignatureServiceConstants.VR_NAMESPACE
						.equals(optionalOutputElement.getNamespaceURI())
						&& "VerificationReport".equals(optionalOutputElement
								.getLocalName())) {
					JAXBElement<VerificationReportType> verificationReportElement;
					try {
						verificationReportElement = (JAXBElement<VerificationReportType>) this.vrUnmarshaller
								.unmarshal(optionalOutputElement);
					} catch (JAXBException e) {
						throw new RuntimeException(
								"JAXB error parsing verification report: "
										+ e.getMessage(), e);
					}
					VerificationReportType verificationReport = verificationReportElement
							.getValue();
					List<IndividualReportType> individualReports = verificationReport
							.getIndividualReport();
					for (IndividualReportType individualReport : individualReports) {
						if (false == DigitalSignatureServiceConstants.RESULT_MAJOR_SUCCESS
								.equals(individualReport.getResult()
										.getResultMajor())) {
							LOG.warn("some invalid VR result reported: "
									+ individualReport.getResult()
											.getResultMajor());
							continue;
						}
						SignedObjectIdentifierType signedObjectIdentifier = individualReport
								.getSignedObjectIdentifier();
						Date signingTime = signedObjectIdentifier
								.getSignedProperties()
								.getSignedSignatureProperties()
								.getSigningTime().toGregorianCalendar()
								.getTime();
						List<Object> details = individualReport.getDetails()
								.getAny();
						X509Certificate signer = null;
						for (Object detail : details) {
							if (detail instanceof JAXBElement<?>) {
								JAXBElement<?> detailElement = (JAXBElement<?>) detail;
								if (new QName(
										DigitalSignatureServiceConstants.VR_NAMESPACE,
										"IndividualCertificateReport")
										.equals(detailElement.getName())) {
									CertificateValidityType individualCertificateReport = (CertificateValidityType) detailElement
											.getValue();
									byte[] encodedSigner = individualCertificateReport
											.getCertificateValue();
									try {
										signer = (X509Certificate) this.certificateFactory
												.generateCertificate(new ByteArrayInputStream(
														encodedSigner));
									} catch (CertificateException e) {
										throw new RuntimeException(
												"cert decoding error: "
														+ e.getMessage(), e);
									}
								}
							}
						}
						if (null == signer) {
							throw new RuntimeException(
									"no signer certificate present in verification report");
						}
						SignatureInfo signatureInfo = new SignatureInfo(signer,
								signingTime);
						signers.add(signatureInfo);
					}
				}
			}
		}

		return signers;
	}

	private ResponseBaseType doVerification(byte[] documentData,
			String mimeType, boolean returnSignerIdentity,
			boolean returnVerificationReport) {
		LOG.debug("verify");
		String requestId = "dss-request-" + UUID.randomUUID().toString();
		DigitalSignatureService digitalSignatureService = DigitalSignatureServiceFactory
				.getInstance();
		DigitalSignatureServicePortType digitalSignatureServicePort = digitalSignatureService
				.getDigitalSignatureServicePort();

		BindingProvider bindingProvider = (BindingProvider) digitalSignatureServicePort;
		bindingProvider.getRequestContext()
				.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
						this.endpointAddress);

		Binding binding = bindingProvider.getBinding();
		List<Handler> handlerChain = binding.getHandlerChain();
		handlerChain.add(new LoggingSoapHandler());
		binding.setHandlerChain(handlerChain);

		VerifyRequest verifyRequest = this.dssObjectFactory
				.createVerifyRequest();
		verifyRequest.setRequestID(requestId);

		AnyType optionalInputs = this.dssObjectFactory.createAnyType();
		if (returnSignerIdentity) {
			JAXBElement<Object> returnSignerIdentityElement = this.dssObjectFactory
					.createReturnSignerIdentity(null);
			optionalInputs.getAny().add(returnSignerIdentityElement);
		}
		if (returnVerificationReport) {
			ReturnVerificationReport jaxbReturnVerificationReport = this.vrObjectFactory
					.createReturnVerificationReport();
			/*
			 * No need to do this, as we're using SSL.
			 */
			jaxbReturnVerificationReport.setIncludeVerifier(false);
			jaxbReturnVerificationReport.setIncludeCertificateValues(true);
			jaxbReturnVerificationReport
					.setReportDetailLevel("urn:oasis:names:tc:dss-x:1.0:profiles:verificationreport:reportdetail:noDetails");

			Document document = this.documentBuilder.newDocument();
			Element newElement = (Element) document.createElement("newNode");
			try {
				this.vrMarshaller.marshal(jaxbReturnVerificationReport,
						newElement);
			} catch (JAXBException e) {
				throw new RuntimeException("JAXB error: " + e.getMessage(), e);
			}
			Element returnVerificationReportElement = (Element) newElement
					.getFirstChild();
			optionalInputs.getAny().add(returnVerificationReportElement);
		}
		if (false == optionalInputs.getAny().isEmpty()) {
			verifyRequest.setOptionalInputs(optionalInputs);
		}

		InputDocuments inputDocuments = this.dssObjectFactory
				.createInputDocuments();
		List<Object> documents = inputDocuments
				.getDocumentOrTransformedDataOrDocumentHash();
		DocumentType document = this.dssObjectFactory.createDocumentType();
		if (null == mimeType || "text/xml".equals(mimeType)) {
			document.setBase64XML(documentData);
		} else {
			Base64Data base64Data = this.dssObjectFactory.createBase64Data();
			base64Data.setValue(documentData);
			base64Data.setMimeType(mimeType);
			document.setBase64Data(base64Data);
		}
		documents.add(document);
		verifyRequest.setInputDocuments(inputDocuments);

		ResponseBaseType responseBase = digitalSignatureServicePort
				.verify(verifyRequest);

		if (null == responseBase) {
			throw new RuntimeException("missing Response");
		}
		String responseRequestId = responseBase.getRequestID();
		if (null == responseRequestId) {
			throw new RuntimeException("missing response RequestID");
		}
		if (false == requestId.equals(responseRequestId)) {
			throw new RuntimeException("incorrect response RequestID");
		}

		return responseBase;
	}
}
