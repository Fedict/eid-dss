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
import java.util.List;
import java.util.Vector;

import javax.ejb.EJB;
import javax.jws.WebService;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

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
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.X509Principal;

import be.fedict.eid.dss.DocumentFormatException;
import be.fedict.eid.dss.InvalidSignatureException;
import be.fedict.eid.dss.SignatureVerificationService;

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
public class DigitalSignatureServicePortImpl implements
		DigitalSignatureServicePortType {

	private static final Log LOG = LogFactory
			.getLog(DigitalSignatureServicePortImpl.class);

	@EJB
	private SignatureVerificationService signatureVerificationService;

	public ResponseBaseType verify(VerifyRequest verifyRequest) {
		LOG.debug("verify");
		ObjectFactory dssObjectFactory = new ObjectFactory();
		oasis.names.tc.saml._1_0.assertion.ObjectFactory samlObjectFactory = new oasis.names.tc.saml._1_0.assertion.ObjectFactory();

		String requestId = verifyRequest.getRequestID();
		LOG.debug("request Id: " + requestId);
		InputDocuments inputDocuments = verifyRequest.getInputDocuments();
		List<Object> documentObjects = inputDocuments
				.getDocumentOrTransformedDataOrDocumentHash();
		if (1 != documentObjects.size()) {
			return createRequestorErrorResponse(dssObjectFactory, requestId);
		}
		Object documentObject = documentObjects.get(0);
		if (false == documentObject instanceof DocumentType) {
			return createRequestorErrorResponse(dssObjectFactory, requestId);
		}
		DocumentType document = (DocumentType) documentObject;
		byte[] xmlData = document.getBase64XML();
		if (null == xmlData) {
			return createRequestorErrorResponse(dssObjectFactory, requestId);
		}

		List<X509Certificate> signatories;
		try {
			signatories = this.signatureVerificationService.verify(xmlData);
		} catch (DocumentFormatException e) {
			return createRequestorErrorResponse(
					dssObjectFactory,
					requestId,
					DigitalSignatureServiceConstants.RESULT_MINOR_NOT_PARSEABLE_XML_DOCUMENT);
		} catch (InvalidSignatureException e) {
			return createRequestorErrorResponse(dssObjectFactory, requestId);
		}

		boolean returnSignerIdentity = false;
		AnyType optionalInput = verifyRequest.getOptionalInputs();
		if (null != optionalInput) {
			List<Object> anyObjects = optionalInput.getAny();
			for (Object anyObject : anyObjects) {
				if (anyObject instanceof JAXBElement<?>) {
					JAXBElement<?> anyElement = (JAXBElement<?>) anyObject;
					if (new QName("urn:oasis:names:tc:dss:1.0:core:schema",
							"ReturnSignerIdentity")
							.equals(anyElement.getName())) {
						returnSignerIdentity = true;
						LOG.debug("ReturnSignerIdentity detected");
					}
				}
			}
		}

		ResponseBaseType responseBase = dssObjectFactory
				.createResponseBaseType();
		responseBase.setRequestID(requestId);
		Result result = dssObjectFactory.createResult();
		result
				.setResultMajor(DigitalSignatureServiceConstants.RESULT_MAJOR_SUCCESS);
		if (signatories.size() > 1) {
			result
					.setResultMinor(DigitalSignatureServiceConstants.RESULT_MINOR_VALID_MULTI_SIGNATURES);
		} else if (1 == signatories.size()) {
			result
					.setResultMinor(DigitalSignatureServiceConstants.RESULT_MINOR_VALID_SIGNATURE);
			if (returnSignerIdentity) {
				NameIdentifierType nameIdentifier = samlObjectFactory
						.createNameIdentifierType();
				X509Certificate signatory = signatories.get(0);
				X509Principal principal;
				try {
					principal = PrincipalUtil
							.getSubjectX509Principal(signatory);
				} catch (CertificateEncodingException e) {
					return createRequestorErrorResponse(dssObjectFactory,
							requestId);
				}
				Vector values = principal.getValues(new DERObjectIdentifier(
						"2.5.4.5"));
				String name = (String) values.get(0);
				nameIdentifier.setValue(name);
				LOG.debug("signer identity: " + name);
				AnyType any = dssObjectFactory.createAnyType();
				any.getAny().add(
						samlObjectFactory.createNameIdentifier(nameIdentifier));
				responseBase.setOptionalOutputs(any);
			}
		} else {
			result
					.setResultMinor(DigitalSignatureServiceConstants.RESULT_MINOR_INVALID_SIGNATURE);
		}
		responseBase.setResult(result);
		return responseBase;
	}

	private ResponseBaseType createRequestorErrorResponse(
			ObjectFactory dssObjectFactory, String requestId) {
		return createRequestorErrorResponse(dssObjectFactory, requestId, null);
	}

	private ResponseBaseType createRequestorErrorResponse(
			ObjectFactory dssObjectFactory, String requestId, String resultMinor) {
		ResponseBaseType responseBase = dssObjectFactory
				.createResponseBaseType();
		responseBase.setRequestID(requestId);
		Result result = dssObjectFactory.createResult();
		result
				.setResultMajor(DigitalSignatureServiceConstants.RESULT_MAJOR_REQUESTER_ERROR);
		if (null != resultMinor) {
			result.setResultMinor(resultMinor);
		}
		responseBase.setResult(result);
		return responseBase;
	}
}
