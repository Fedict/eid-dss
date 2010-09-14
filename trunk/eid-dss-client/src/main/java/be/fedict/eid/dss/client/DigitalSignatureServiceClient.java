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

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
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
import be.fedict.eid.dss.ws.jaxb.dss.DocumentType;
import be.fedict.eid.dss.ws.jaxb.dss.InputDocuments;
import be.fedict.eid.dss.ws.jaxb.dss.ObjectFactory;
import be.fedict.eid.dss.ws.jaxb.dss.ResponseBaseType;
import be.fedict.eid.dss.ws.jaxb.dss.Result;
import be.fedict.eid.dss.ws.jaxb.dss.VerifyRequest;
import be.fedict.eid.dss.ws.jaxb.saml.NameIdentifierType;
import be.fedict.eid.dss.ws.profile.vr.jaxb.ReturnVerificationReport;

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

	private final DocumentBuilder documentBuilder;

	public DigitalSignatureServiceClient(String endpointAddress) {
		this.endpointAddress = endpointAddress;
		this.dssObjectFactory = new ObjectFactory();
		this.vrObjectFactory = new be.fedict.eid.dss.ws.profile.vr.jaxb.ObjectFactory();
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
	}

	public DigitalSignatureServiceClient() {
		this(DEFAULT_ENDPOINT_ADDRESS);
	}

	public boolean verify(String signedDocument)
			throws NotParseableXMLDocumentException {
		ResponseBaseType responseBase = doVerification(signedDocument, false,
				false);

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

	public List<X509Certificate> verifyWithSigners(String signedDocument) {
		ResponseBaseType responseBase = doVerification(signedDocument, false,
				true);

		Result result = responseBase.getResult();
		String resultMajor = result.getResultMajor();
		LOG.debug("result major: " + resultMajor);
		if (false == DigitalSignatureServiceConstants.RESULT_MAJOR_SUCCESS
				.equals(resultMajor)) {
			throw new RuntimeException("unsuccessful result: " + resultMajor);
		}

		return null;
	}

	/**
	 * Verifies the signature on a signed XML document.
	 * 
	 * @param signedDocument
	 *            the document.
	 * @return the identifier of the signatory.
	 */
	public String verifyWithSignerIdentity(String signedDocument) {
		ResponseBaseType responseBase = doVerification(signedDocument, true,
				false);

		Result result = responseBase.getResult();
		String resultMajor = result.getResultMajor();
		LOG.debug("result major: " + resultMajor);
		if (false == DigitalSignatureServiceConstants.RESULT_MAJOR_SUCCESS
				.equals(resultMajor)) {
			throw new RuntimeException("unsuccessful result: " + resultMajor);
		}
		String resultMinor = result.getResultMinor();
		if (null == resultMinor) {
			throw new RuntimeException("missing ResultMinor");
		}

		if (DigitalSignatureServiceConstants.RESULT_MINOR_VALID_SIGNATURE
				.equals(resultMinor)) {
			AnyType anyType = responseBase.getOptionalOutputs();
			if (null == anyType) {
				throw new RuntimeException("expected OptionalOutputs");
			}
			List<Object> anyList = anyType.getAny();
			for (Object anyObject : anyList) {
				if (anyObject instanceof JAXBElement<?>) {
					JAXBElement<?> jaxbElement = (JAXBElement<?>) anyObject;
					Object value = jaxbElement.getValue();
					if (value instanceof NameIdentifierType) {
						NameIdentifierType nameIdentifier = (NameIdentifierType) value;
						String name = nameIdentifier.getValue();
						LOG.debug("identifier: " + name);
						return name;
					}
				}
				if (anyObject instanceof Element) {
					Element element = (Element) anyObject;
					if ("NameIdentifier".equals(element.getLocalName())) {
						String name = element.getTextContent();
						if (name != null) {
							name = name.trim();
							LOG.debug("identifier: " + name);
							return name;
						}
					}
				}
			}
			return null;
		}
		if (DigitalSignatureServiceConstants.RESULT_MINOR_VALID_MULTI_SIGNATURES
				.equals(resultMinor)) {
			throw new UnsupportedOperationException();
		}
		return null;
	}

	private ResponseBaseType doVerification(String signedDocument,
			boolean returnSignerIdentity, boolean returnVerificationReport) {
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
		document.setBase64XML(signedDocument.getBytes());
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
