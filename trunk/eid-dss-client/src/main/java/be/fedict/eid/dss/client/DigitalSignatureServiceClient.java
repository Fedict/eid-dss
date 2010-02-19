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

import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBElement;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;

import oasis.names.tc.dss._1_0.core.schema.AnyType;
import oasis.names.tc.dss._1_0.core.schema.DocumentType;
import oasis.names.tc.dss._1_0.core.schema.InputDocuments;
import oasis.names.tc.dss._1_0.core.schema.ObjectFactory;
import oasis.names.tc.dss._1_0.core.schema.ResponseBaseType;
import oasis.names.tc.dss._1_0.core.schema.Result;
import oasis.names.tc.dss._1_0.core.schema.VerifyRequest;
import oasis.names.tc.saml._1_0.assertion.NameIdentifierType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.ws.DigitalSignatureService;
import be.fedict.eid.dss.ws.DigitalSignatureServiceConstants;
import be.fedict.eid.dss.ws.DigitalSignatureServiceFactory;
import be.fedict.eid.dss.ws.DigitalSignatureServicePortType;

/**
 * Client for the OASIS DSS verification web service.
 * 
 * @author Frank Cornelis
 * 
 */
public class DigitalSignatureServiceClient {

	private static final Log LOG = LogFactory
			.getLog(DigitalSignatureServiceClient.class);

	public boolean verify(String signedDocument)
			throws NotParseableXMLDocumentException {
		ObjectFactory dssObjectFactory = new ObjectFactory();
		String requestId = "dss-request-" + UUID.randomUUID().toString();
		ResponseBaseType responseBase = doVerification(dssObjectFactory,
				requestId, signedDocument, false);
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
	 * Verifies the signature on a signed XML document.
	 * 
	 * @param signedDocument
	 *            the document.
	 * @return the identifier of the signatory.
	 */
	public String verifyWithSignerIdentity(String signedDocument) {
		ObjectFactory dssObjectFactory = new ObjectFactory();
		String requestId = "dss-request-" + UUID.randomUUID().toString();
		ResponseBaseType responseBase = doVerification(dssObjectFactory,
				requestId, signedDocument, true);

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
			}
			return null;
		}
		if (DigitalSignatureServiceConstants.RESULT_MINOR_VALID_MULTI_SIGNATURES
				.equals(resultMinor)) {
			throw new UnsupportedOperationException();
		}
		return null;
	}

	private ResponseBaseType doVerification(ObjectFactory dssObjectFactory,
			String requestId, String signedDocument,
			boolean returnSignerIdentity) {
		LOG.debug("verify");
		DigitalSignatureService digitalSignatureService = DigitalSignatureServiceFactory
				.getInstance();
		DigitalSignatureServicePortType digitalSignatureServicePort = digitalSignatureService
				.getDigitalSignatureServicePort();

		BindingProvider bindingProvider = (BindingProvider) digitalSignatureServicePort;
		bindingProvider.getRequestContext().put(
				BindingProvider.ENDPOINT_ADDRESS_PROPERTY,
				"http://localhost:8080/eid-dss-ws/dss");

		Binding binding = bindingProvider.getBinding();
		List<Handler> handlerChain = binding.getHandlerChain();
		handlerChain.add(new LoggingSoapHandler());
		binding.setHandlerChain(handlerChain);

		VerifyRequest verifyRequest = dssObjectFactory.createVerifyRequest();
		verifyRequest.setRequestID(requestId);
		if (returnSignerIdentity) {
			JAXBElement<Object> returnSignerIdentityElement = dssObjectFactory
					.createReturnSignerIdentity(null);
			AnyType anyType = dssObjectFactory.createAnyType();
			anyType.getAny().add(returnSignerIdentityElement);
			verifyRequest.setOptionalInputs(anyType);
		}
		InputDocuments inputDocuments = dssObjectFactory.createInputDocuments();
		List<Object> documents = inputDocuments
				.getDocumentOrTransformedDataOrDocumentHash();
		DocumentType document = dssObjectFactory.createDocumentType();
		document.setBase64XML(signedDocument.getBytes());
		documents.add(document);
		verifyRequest.setInputDocuments(inputDocuments);

		ResponseBaseType responseBase = digitalSignatureServicePort
				.verify(verifyRequest);
		return responseBase;
	}
}
