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

import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.handler.Handler;

import oasis.names.tc.dss._1_0.core.schema.DocumentType;
import oasis.names.tc.dss._1_0.core.schema.InputDocuments;
import oasis.names.tc.dss._1_0.core.schema.ObjectFactory;
import oasis.names.tc.dss._1_0.core.schema.ResponseBaseType;
import oasis.names.tc.dss._1_0.core.schema.Result;
import oasis.names.tc.dss._1_0.core.schema.VerifyRequest;

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

	public boolean verify(String signedDocument) {
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

		ObjectFactory dssObjectFactory = new ObjectFactory();

		VerifyRequest verifyRequest = dssObjectFactory.createVerifyRequest();
		String requestId = "dss-request-" + UUID.randomUUID().toString();
		verifyRequest.setRequestID(requestId);
		InputDocuments inputDocuments = dssObjectFactory.createInputDocuments();
		List<Object> documents = inputDocuments
				.getDocumentOrTransformedDataOrDocumentHash();
		DocumentType document = dssObjectFactory.createDocumentType();
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
		return DigitalSignatureServiceConstants.RESULT_MINOR_VALID_SIGNATURE
				.equals(resultMinor);
	}
}
