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
import java.net.ProxySelector;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Iterator;
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

import be.fedict.eid.dss.ws.DSSConstants;
import be.fedict.eid.dss.ws.DigitalSignatureService;
import be.fedict.eid.dss.ws.DigitalSignatureServiceFactory;
import be.fedict.eid.dss.ws.DigitalSignatureServicePortType;
import be.fedict.eid.dss.ws.jaxb.dss.AnyType;
import be.fedict.eid.dss.ws.jaxb.dss.Base64Data;
import be.fedict.eid.dss.ws.jaxb.dss.DocumentType;
import be.fedict.eid.dss.ws.jaxb.dss.DocumentWithSignature;
import be.fedict.eid.dss.ws.jaxb.dss.ObjectFactory;
import be.fedict.eid.dss.ws.jaxb.dss.ResponseBaseType;
import be.fedict.eid.dss.ws.jaxb.dss.Result;
import be.fedict.eid.dss.ws.jaxb.dss.SignRequest;
import be.fedict.eid.dss.ws.jaxb.dss.SignResponse;
import be.fedict.eid.dss.ws.jaxb.dss.VerifyRequest;
import be.fedict.eid.dss.ws.profile.artifact.jaxb.ReturnStoredDocument;
import be.fedict.eid.dss.ws.profile.artifact.jaxb.StorageInfo;
import be.fedict.eid.dss.ws.profile.originaldocument.jaxb.OriginalDocumentType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.CertificateValidityType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.DetailedSignatureReportType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.IndividualReportType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.PropertiesType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.ReturnVerificationReport;
import be.fedict.eid.dss.ws.profile.vr.jaxb.SignedObjectIdentifierType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.SignerRoleType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.VerificationReportType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.dss.InputDocuments;

/**
 * Client for the OASIS DSS verification web service.
 * 
 * @author Frank Cornelis
 */
public class DigitalSignatureServiceClient {

	public static final String DEFAULT_ENDPOINT_ADDRESS = "http://localhost:8080/eid-dss-ws/dss";

	private static final Log LOG = LogFactory
			.getLog(DigitalSignatureServiceClient.class);

	private static final QName detailedSignatureReportQName = new QName(
			DSSConstants.VR_NAMESPACE, "DetailedSignatureReport");

	private DigitalSignatureServicePortType port;

	private final String endpointAddress;

	private final ObjectFactory dssObjectFactory;
	private final be.fedict.eid.dss.ws.profile.vr.jaxb.ObjectFactory vrObjectFactory;
	private final be.fedict.eid.dss.ws.profile.vr.jaxb.dss.ObjectFactory vrDssObjectFactory = new be.fedict.eid.dss.ws.profile.vr.jaxb.dss.ObjectFactory();
	private final be.fedict.eid.dss.ws.profile.artifact.jaxb.ObjectFactory artifactObjectFactory = new be.fedict.eid.dss.ws.profile.artifact.jaxb.ObjectFactory();
	private final be.fedict.eid.dss.ws.profile.originaldocument.jaxb.ObjectFactory originalDocumentObjectFactory = new be.fedict.eid.dss.ws.profile.originaldocument.jaxb.ObjectFactory();

	private final Marshaller vrMarshaller;
	private final Unmarshaller vrUnmarshaller;

	private final Marshaller artifactMarshaller;
	private final Unmarshaller artifactUnmarshaller;

	private final Marshaller originalDocumentMarshaller;

	private final DocumentBuilder documentBuilder;

	private final CertificateFactory certificateFactory;

	private static DSSProxySelector proxySelector;

	static {
		ProxySelector defaultProxySelector = ProxySelector.getDefault();
		DigitalSignatureServiceClient.proxySelector = new DSSProxySelector(
				defaultProxySelector);
		ProxySelector.setDefault(DigitalSignatureServiceClient.proxySelector);
	}

	/**
	 * Main constructor.
	 * 
	 * @param endpointAddress
	 *            the DSS web service endpoint address. For example
	 *            http://localhost:8080/eid-dss-ws/dss
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

			JAXBContext artifactJAXBContext = JAXBContext
					.newInstance(be.fedict.eid.dss.ws.profile.artifact.jaxb.ObjectFactory.class);
			this.artifactMarshaller = artifactJAXBContext.createMarshaller();
			this.artifactUnmarshaller = artifactJAXBContext
					.createUnmarshaller();

			JAXBContext originalDocumentJAXBContext = JAXBContext
					.newInstance(be.fedict.eid.dss.ws.profile.originaldocument.jaxb.ObjectFactory.class);
			this.originalDocumentMarshaller = originalDocumentJAXBContext
					.createMarshaller();
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

		this.port = getPort();
	}

	/**
	 * Default constructor.
	 * <p/>
	 * Will connect to http://localhost:8080/eid-dss-ws/dss
	 */
	public DigitalSignatureServiceClient() {
		this(DEFAULT_ENDPOINT_ADDRESS);
	}

	private DigitalSignatureServicePortType getPort() {

		DigitalSignatureService digitalSignatureService = DigitalSignatureServiceFactory
				.getInstance();
		DigitalSignatureServicePortType digitalSignatureServicePort = digitalSignatureService
				.getDigitalSignatureServicePort();

		BindingProvider bindingProvider = (BindingProvider) digitalSignatureServicePort;
		bindingProvider.getRequestContext()
				.put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
						this.endpointAddress);
		return digitalSignatureServicePort;
	}

	/**
	 * Proxy configuration setting ( both http as https ).
	 * 
	 * @param proxyHost
	 *            proxy hostname
	 * @param proxyPort
	 *            proxy port
	 */
	public void setProxy(String proxyHost, int proxyPort) {
		DigitalSignatureServiceClient.proxySelector.setProxy(
				this.endpointAddress, proxyHost, proxyPort);
	}

	/**
	 * Enables/disables logging of all SOAP requests/responses.
	 * 
	 * @param logging
	 *            logging or not
	 * @param logToFile
	 *            log to file if logging is enabled
	 */
	public void setLogging(boolean logging, boolean logToFile) {

		if (logging) {
			registerLoggerHandler(logToFile);
		} else {
			removeLoggerHandler();
		}
	}

	/**
	 * Registers the logging SOAP handler on the given JAX-WS port component.
	 * 
	 * @param logToFile
	 *            log to file or not
	 */
	protected void registerLoggerHandler(boolean logToFile) {

		BindingProvider bindingProvider = (BindingProvider) this.port;

		Binding binding = bindingProvider.getBinding();
		List<Handler> handlerChain = binding.getHandlerChain();
		handlerChain.add(new LoggingSoapHandler(logToFile));
		binding.setHandlerChain(handlerChain);
	}

	/**
	 * Unregister possible logging SOAP handlers on the given JAX-WS port
	 * component.
	 */
	protected void removeLoggerHandler() {

		BindingProvider bindingProvider = (BindingProvider) this.port;

		Binding binding = bindingProvider.getBinding();
		List<Handler> handlerChain = binding.getHandlerChain();
		Iterator<Handler> iter = handlerChain.iterator();
		while (iter.hasNext()) {
			Handler handler = iter.next();
			if (handler instanceof LoggingSoapHandler) {
				iter.remove();
			}

		}
	}

	/**
	 * Verifies whether the given document has been signed or not.
	 * 
	 * @param signedDocument
	 *            signed document to verify
	 * @param mimeType
	 *            optional mime-type, default is "text/xml".
	 * @return <code>true</code> is the document has been signed,
	 *         <code>false</code> otherwise.
	 * @throws NotParseableXMLDocumentException
	 *             XML document was not parseable.
	 */
	public boolean verify(byte[] signedDocument, String mimeType)
			throws NotParseableXMLDocumentException {
		return verify(signedDocument, mimeType, null);
	}

	/**
	 * Verifies whether the given document has been signed or not.
	 * 
	 * @param signedDocument
	 *            signed document to verify
	 * @param mimeType
	 *            optional mime-type, default is "text/xml".
	 * @param originalDocument
	 *            the optional original document.
	 * @return <code>true</code> is the document has been signed,
	 *         <code>false</code> otherwise.
	 * @throws NotParseableXMLDocumentException
	 *             XML document was not parseable.
	 */
	public boolean verify(byte[] signedDocument, String mimeType,
			byte[] originalDocument) throws NotParseableXMLDocumentException {

		ResponseBaseType response = doVerification(signedDocument, mimeType,
				false, false, originalDocument);

		String resultMinor = validateResult(response);
		if (null == resultMinor) {
			throw new RuntimeException("missing ResultMinor");
		}
		if (DSSConstants.RESULT_MINOR_VALID_SIGNATURE.equals(resultMinor)) {
			return true;
		}
		if (DSSConstants.RESULT_MINOR_VALID_MULTI_SIGNATURES
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
	 *            signed document to verify
	 * @param mimeType
	 *            optional mime-type, default is "text/xml".
	 * @return a list of signature information objects detailing on the signing
	 *         parties.
	 * @throws NotParseableXMLDocumentException
	 *             XML document was not parseable.
	 */
	public List<SignatureInfo> verifyWithSigners(byte[] signedDocument,
			String mimeType) throws NotParseableXMLDocumentException {
		return verifyWithSigners(signedDocument, mimeType, null);
	}

	/**
	 * Verifies whether the given document has been signed and reports back on
	 * the signing parties.
	 * 
	 * @param signedDocument
	 *            signed document to verify
	 * @param mimeType
	 *            optional mime-type, default is "text/xml".
	 * @param originalDocument
	 *            the optional original document.
	 * @return a list of signature information objects detailing on the signing
	 *         parties.
	 * @throws NotParseableXMLDocumentException
	 *             XML document was not parseable.
	 */
	public List<SignatureInfo> verifyWithSigners(byte[] signedDocument,
			String mimeType, byte[] originalDocument)
			throws NotParseableXMLDocumentException {

		ResponseBaseType responseBase = doVerification(signedDocument,
				mimeType, false, true, originalDocument);

		validateResult(responseBase);

		// parse VerificationReport
		List<SignatureInfo> signers = new LinkedList<SignatureInfo>();
		VerificationReportType verificationReport = findVerificationReport(responseBase);
		if (null == verificationReport) {
			return signers;
		}

		List<IndividualReportType> individualReports = verificationReport
				.getIndividualReport();
		for (IndividualReportType individualReport : individualReports) {

			if (!DSSConstants.RESULT_MAJOR_SUCCESS.equals(individualReport
					.getResult().getResultMajor())) {
				LOG.warn("some invalid VR result reported: "
						+ individualReport.getResult().getResultMajor());
				continue;
			}
			SignedObjectIdentifierType signedObjectIdentifier = individualReport
					.getSignedObjectIdentifier();
			Date signingTime = signedObjectIdentifier.getSignedProperties()
					.getSignedSignatureProperties().getSigningTime()
					.toGregorianCalendar().getTime();

			List<Object> details = individualReport.getDetails().getAny();
			X509Certificate signer = null;
			String role = null;
			for (Object detail : details) {
				if (detail instanceof JAXBElement<?>) {
					JAXBElement<?> detailElement = (JAXBElement<?>) detail;
					if (detailedSignatureReportQName.equals(detailElement
							.getName())) {
						DetailedSignatureReportType detailedSignatureReport = (DetailedSignatureReportType) detailElement
								.getValue();

						List<CertificateValidityType> certificateValidities = detailedSignatureReport
								.getCertificatePathValidity()
								.getPathValidityDetail()
								.getCertificateValidity();
						CertificateValidityType certificateValidity = certificateValidities
								.get(0);
						byte[] encodedSigner = certificateValidity
								.getCertificateValue();
						try {
							signer = (X509Certificate) this.certificateFactory
									.generateCertificate(new ByteArrayInputStream(
											encodedSigner));
						} catch (CertificateException e) {
							throw new RuntimeException("cert decoding error: "
									+ e.getMessage(), e);
						}

						PropertiesType properties = detailedSignatureReport
								.getProperties();
						if (null != properties) {
							SignerRoleType signerRole = properties
									.getSignedProperties()
									.getSignedSignatureProperties()
									.getSignerRole();
							if (null != signerRole) {
								role = (String) signerRole.getClaimedRoles()
										.getClaimedRole().get(0).getContent()
										.get(0);
							}
						}
					}
				}
			}
			if (null == signer) {
				throw new RuntimeException(
						"no signer certificate present in verification report");
			}
			SignatureInfo signatureInfo = new SignatureInfo(signer,
					signingTime, role);
			signers.add(signatureInfo);
		}

		return signers;
	}

	private ResponseBaseType doVerification(byte[] documentData,
			String mimeType, boolean returnSignerIdentity,
			boolean returnVerificationReport, byte[] originalDocumentData)
			throws NotParseableXMLDocumentException {

		LOG.debug("verify");

		String requestId = "dss-verify-request-" + UUID.randomUUID().toString();
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
			Element newElement = document.createElement("newNode");
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
		if (null != originalDocumentData) {
			OriginalDocumentType originalDocument = this.originalDocumentObjectFactory
					.createOriginalDocumentType();
			InputDocuments inputDocuments = this.vrDssObjectFactory
					.createInputDocuments();
			List<Object> documents = inputDocuments
					.getDocumentOrTransformedDataOrDocumentHash();
			be.fedict.eid.dss.ws.profile.vr.jaxb.dss.DocumentType document = this.vrDssObjectFactory
					.createDocumentType();
			if (null == mimeType || "text/xml".equals(mimeType)) {
				document.setBase64XML(originalDocumentData);
			} else {
				be.fedict.eid.dss.ws.profile.vr.jaxb.dss.Base64Data base64Data = this.vrDssObjectFactory
						.createBase64Data();
				base64Data.setValue(originalDocumentData);
				base64Data.setMimeType(mimeType);
				document.setBase64Data(base64Data);
			}
			documents.add(document);
			originalDocument.setInputDocuments(inputDocuments);

			Document domDocument = this.documentBuilder.newDocument();
			Element newElement = domDocument.createElement("newElement");
			try {
				this.originalDocumentMarshaller.marshal(
						this.originalDocumentObjectFactory
								.createOriginalDocument(originalDocument),
						newElement);
			} catch (JAXBException e) {
				throw new RuntimeException("JAXB error: " + e.getMessage(), e);
			}

			optionalInputs.getAny().add((Element) newElement.getFirstChild());
		}

		if (!optionalInputs.getAny().isEmpty()) {
			verifyRequest.setOptionalInputs(optionalInputs);
		}

		verifyRequest.setInputDocuments(getInputDocuments(documentData,
				mimeType));

		// operate
		ResponseBaseType response = port.verify(verifyRequest);

		// check response
		checkResponse(response, requestId);

		return response;
	}

	/**
	 * Send specified document to the eID DSS WS for temp storage. The WS will
	 * return {@link StorageInfoDO} containing the artifact ID for the uploaded
	 * document.
	 * 
	 * @param documentData
	 *            document to be signed
	 * @param contentType
	 *            content type of document to be signed.
	 * @return document storage information object.
	 */
	public StorageInfoDO store(byte[] documentData, String contentType) {

		// create request
		String requestId = "dss-sign-request-" + UUID.randomUUID().toString();
		SignRequest signRequest = this.dssObjectFactory.createSignRequest();
		signRequest.setRequestID(requestId);
		signRequest.setProfile(DSSConstants.ARTIFACT_NAMESPACE);

		// add "ReturnStoreInfo" optional input
		AnyType optionalInputs = this.dssObjectFactory.createAnyType();
		JAXBElement<Object> returnStorageInfoElement = this.artifactObjectFactory
				.createReturnStorageInfo(null);
		optionalInputs.getAny().add(returnStorageInfoElement);
		signRequest.setOptionalInputs(optionalInputs);

		// add document
		signRequest.setInputDocuments(getInputDocuments(documentData,
				contentType));

		// operate
		SignResponse signResponse = this.port.sign(signRequest);

		// parse response
		checkResponse(signResponse, requestId);

		try {
			validateResult(signResponse);
		} catch (NotParseableXMLDocumentException e) {
			throw new RuntimeException(e);
		}

		// check profile
		if (!signResponse.getProfile().equals(DSSConstants.ARTIFACT_NAMESPACE)) {
			throw new RuntimeException("Unexpected SignResponse.profile: "
					+ signResponse.getProfile());
		}

		// parse StorageInfo
		StorageInfo storageInfo = findStorageInfo(signResponse);
		if (null == storageInfo) {
			throw new RuntimeException("Missing StorageInfo");
		}

		Date notBefore = storageInfo.getValidity().getNotBefore()
				.toGregorianCalendar().getTime();
		Date notAfter = storageInfo.getValidity().getNotAfter()
				.toGregorianCalendar().getTime();
		String artifact = storageInfo.getIdentifier();

		LOG.debug("Artifact: " + artifact + " notBefore=" + notBefore
				+ " notAfter=" + notAfter);

		return new StorageInfoDO(artifact, notBefore, notAfter);
	}

	/**
	 * Retrieve the document specified by the given ID from the eID DSS service.
	 * 
	 * @param documentId
	 *            the ID of the document to fetch.
	 * @return the decoded document data.
	 * @throws DocumentNotFoundException
	 *             no document was returned
	 */
	public byte[] retrieve(String documentId) throws DocumentNotFoundException {

		// create request
		String requestId = "dss-sign-request-" + UUID.randomUUID().toString();
		SignRequest signRequest = this.dssObjectFactory.createSignRequest();
		signRequest.setRequestID(requestId);
		signRequest.setProfile(DSSConstants.ARTIFACT_NAMESPACE);

		// add "ReturnStoredDocument" optional input
		AnyType optionalInputs = this.dssObjectFactory.createAnyType();
		ReturnStoredDocument returnStoredDocument = this.artifactObjectFactory
				.createReturnStoredDocument();
		returnStoredDocument.setIdentifier(documentId);
		optionalInputs.getAny().add(
				getReturnStoredDocumentElement(returnStoredDocument));
		signRequest.setOptionalInputs(optionalInputs);

		// operate
		SignResponse signResponse = this.port.sign(signRequest);

		// parse response
		checkResponse(signResponse, requestId);

		try {
			validateResult(signResponse);
		} catch (NotParseableXMLDocumentException e) {
			throw new RuntimeException(e);
		}

		// check profile
		if (!signResponse.getProfile().equals(DSSConstants.ARTIFACT_NAMESPACE)) {
			throw new RuntimeException("Unexpected SignResponse.profile: "
					+ signResponse.getProfile());
		}

		// get document
		DocumentWithSignature documentWithSignature = findDocumentWithSignature(signResponse);
		if (null == documentWithSignature
				|| null == documentWithSignature.getDocument()) {
			throw new DocumentNotFoundException();
		}
		Base64Data base64Data = documentWithSignature.getDocument()
				.getBase64Data();
		byte[] documentData;
		if (null != base64Data) {
			documentData = base64Data.getValue();
		} else {
			documentData = documentWithSignature.getDocument().getBase64XML();
		}

		if (null == documentData) {
			throw new DocumentNotFoundException();
		}
		return documentData;
	}

	public Element getReturnStoredDocumentElement(
			ReturnStoredDocument returnStoredDocument) {

		Document newDocument = this.documentBuilder.newDocument();
		Element newElement = newDocument.createElement("newNode");
		try {
			this.artifactMarshaller.marshal(this.artifactObjectFactory
					.createReturnStoredDocument(returnStoredDocument),
					newElement);
		} catch (JAXBException e) {
			throw new RuntimeException("JAXB error: " + e.getMessage(), e);
		}
		return (Element) newElement.getFirstChild();
	}

	private be.fedict.eid.dss.ws.jaxb.dss.InputDocuments getInputDocuments(
			byte[] documentData, String mimeType) {

		be.fedict.eid.dss.ws.jaxb.dss.InputDocuments inputDocuments = this.dssObjectFactory
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
		return inputDocuments;
	}

	private void checkResponse(ResponseBaseType response, String requestId) {

		if (null == response) {
			throw new RuntimeException("No response returned!");
		}
		String responseRequestId = response.getRequestID();
		if (null == responseRequestId) {
			throw new RuntimeException("missing response RequestID");
		}
		if (!requestId.equals(responseRequestId)) {
			throw new RuntimeException("incorrect response RequestID");
		}
	}

	private String validateResult(ResponseBaseType response)
			throws NotParseableXMLDocumentException {

		Result result = response.getResult();
		String resultMajor = result.getResultMajor();
		LOG.debug("result major: " + resultMajor);
		String resultMinor = result.getResultMinor();
		if (!DSSConstants.RESULT_MAJOR_SUCCESS.equals(resultMajor)) {
			LOG.warn("result minor: " + resultMinor);
			if (null != resultMinor
					&& DSSConstants.RESULT_MINOR_NOT_PARSEABLE_XML_DOCUMENT
							.equals(resultMinor)) {
				throw new NotParseableXMLDocumentException();
			}
			throw new RuntimeException("unsuccessful result: " + resultMajor);
		}
		return resultMinor;

	}

	@SuppressWarnings("unchecked")
	private VerificationReportType findVerificationReport(
			ResponseBaseType responseBase) {

		AnyType optionalOutputs = responseBase.getOptionalOutputs();
		if (null == optionalOutputs) {
			return null;
		}
		List<Object> optionalOutputContent = optionalOutputs.getAny();
		for (Object optionalOutput : optionalOutputContent) {

			if (optionalOutput instanceof Element) {
				Element optionalOutputElement = (Element) optionalOutput;
				if (DSSConstants.VR_NAMESPACE.equals(optionalOutputElement
						.getNamespaceURI())
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
					return verificationReportElement.getValue();
				}
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private StorageInfo findStorageInfo(SignResponse signeResponse) {

		AnyType optionalOutputs = signeResponse.getOptionalOutputs();
		if (null == optionalOutputs) {
			return null;
		}
		List<Object> optionalOutputContent = optionalOutputs.getAny();
		for (Object optionalOutput : optionalOutputContent) {

			if (optionalOutput instanceof Element) {

				Element optionalOutputElement = (Element) optionalOutput;
				if (DSSConstants.ARTIFACT_NAMESPACE
						.equals(optionalOutputElement.getNamespaceURI())
						&& "StorageInfo".equals(optionalOutputElement
								.getLocalName())) {
					JAXBElement<StorageInfo> storageInfoElement;
					try {
						storageInfoElement = (JAXBElement<StorageInfo>) this.artifactUnmarshaller
								.unmarshal(optionalOutputElement);
					} catch (JAXBException e) {
						throw new RuntimeException(
								"JAXB error parsing storage info: "
										+ e.getMessage(), e);
					}
					return storageInfoElement.getValue();
				}
			}
		}

		return null;
	}

	@SuppressWarnings("unchecked")
	private DocumentWithSignature findDocumentWithSignature(
			SignResponse signeResponse) {

		AnyType optionalOutputs = signeResponse.getOptionalOutputs();
		if (null == optionalOutputs) {
			return null;
		}
		List<Object> optionalOutputContent = optionalOutputs.getAny();
		for (Object optionalOutput : optionalOutputContent) {

			if (optionalOutput instanceof DocumentWithSignature) {
				return (DocumentWithSignature) optionalOutput;
			}
		}

		return null;
	}
}
