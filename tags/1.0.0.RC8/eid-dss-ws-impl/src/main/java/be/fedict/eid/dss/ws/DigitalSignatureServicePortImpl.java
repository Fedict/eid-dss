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

import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.ejb.EJB;
import javax.jws.WebService;
import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.w3c.dom.Element;

import be.fedict.eid.dss.entity.DocumentEntity;
import be.fedict.eid.dss.model.DocumentService;
import be.fedict.eid.dss.model.SignatureVerificationService;
import be.fedict.eid.dss.model.exception.DocumentFormatException;
import be.fedict.eid.dss.model.exception.InvalidSignatureException;
import be.fedict.eid.dss.spi.SignatureInfo;
import be.fedict.eid.dss.ws.jaxb.dss.AnyType;
import be.fedict.eid.dss.ws.jaxb.dss.Base64Data;
import be.fedict.eid.dss.ws.jaxb.dss.DocumentType;
import be.fedict.eid.dss.ws.jaxb.dss.DocumentWithSignature;
import be.fedict.eid.dss.ws.jaxb.dss.InputDocuments;
import be.fedict.eid.dss.ws.jaxb.dss.ObjectFactory;
import be.fedict.eid.dss.ws.jaxb.dss.ResponseBaseType;
import be.fedict.eid.dss.ws.jaxb.dss.Result;
import be.fedict.eid.dss.ws.jaxb.dss.SignRequest;
import be.fedict.eid.dss.ws.jaxb.dss.SignResponse;
import be.fedict.eid.dss.ws.jaxb.dss.VerifyRequest;
import be.fedict.eid.dss.ws.profile.artifact.jaxb.ReturnStoredDocument;
import be.fedict.eid.dss.ws.profile.artifact.jaxb.StorageInfo;
import be.fedict.eid.dss.ws.profile.artifact.jaxb.ValidityType;

import com.sun.xml.ws.developer.UsesJAXBContext;

/**
 * Implementation of the DSS verification web service JAX-WS endpoint.
 * <p/>
 * TODO: we can use this directly as servlet in web.xml. Before doing so we
 * should activate the Metro JBossWS.
 * 
 * @author Frank Cornelis
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

	@EJB
	private DocumentService documentService;

	@Override
	public ResponseBaseType verify(VerifyRequest verifyRequest) {

		LOG.debug("verify");

		/*
		 * Parse the request.
		 */
		String requestId = verifyRequest.getRequestID();
		LOG.debug("request Id: " + requestId);
		List<DocumentDO> documents = getDocuments(verifyRequest
				.getInputDocuments());

		if (documents.isEmpty()) {
			return DSSUtil.createRequestorErrorResponse(requestId, null,
					"No valid document found to validate.");
		}
		if (documents.size() != 1) {
			return DSSUtil.createRequestorErrorResponse(requestId, null,
					"Can validate only one document.");
		}
		byte[] data = documents.get(0).getDocumentData();
		String mimeType = documents.get(0).getContentType();

		byte[] originalData = null;
		boolean returnVerificationReport = false;
		AnyType optionalInput = verifyRequest.getOptionalInputs();
		if (null != optionalInput) {
			List<Object> anyObjects = optionalInput.getAny();
			for (Object anyObject : anyObjects) {
				if (anyObject instanceof Element) {
					Element element = (Element) anyObject;
					if (DSSConstants.VR_NAMESPACE.equals(element
							.getNamespaceURI())
							&& "ReturnVerificationReport".equals(element
									.getLocalName())) {
						/*
						 * We could check for supported ReturnVerificationReport
						 * input, but then again, who cares?
						 */
						returnVerificationReport = true;
					}
					if (DSSConstants.ORIGINAL_DOCUMENT_NAMESPACE.equals(element
							.getNamespaceURI())
							&& DSSConstants.ORIGINAL_DOCUMENT_ELEMENT
									.equals(element.getLocalName())) {
						try {
							originalData = DSSUtil.getOriginalDocument(element);
						} catch (JAXBException e) {
							return DSSUtil
									.createRequestorErrorResponse(
											requestId,
											DSSConstants.RESULT_MINOR_NOT_PARSEABLE_XML_DOCUMENT);
						}
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
					mimeType, originalData);
		} catch (DocumentFormatException e) {
			return DSSUtil.createRequestorErrorResponse(requestId,
					DSSConstants.RESULT_MINOR_NOT_PARSEABLE_XML_DOCUMENT);
		} catch (InvalidSignatureException e) {
			return DSSUtil.createRequestorErrorResponse(requestId);
		}

		/*
		 * Construct the DSS response.
		 */
		ObjectFactory dssObjectFactory = new ObjectFactory();

		ResponseBaseType responseBase = dssObjectFactory
				.createResponseBaseType();
		responseBase.setRequestID(requestId);
		Result result = dssObjectFactory.createResult();
		result.setResultMajor(DSSConstants.RESULT_MAJOR_SUCCESS);
		AnyType optionalOutput = dssObjectFactory.createAnyType();
		if (signatureInfos.size() > 1) {
			result.setResultMinor(DSSConstants.RESULT_MINOR_VALID_MULTI_SIGNATURES);
		} else if (1 == signatureInfos.size()) {
			result.setResultMinor(DSSConstants.RESULT_MINOR_VALID_SIGNATURE);
		} else {
			result.setResultMinor(DSSConstants.RESULT_MINOR_INVALID_SIGNATURE);
		}

		if (returnVerificationReport) {
			DSSUtil.addVerificationReport(optionalOutput, signatureInfos);
		}

		if (!optionalOutput.getAny().isEmpty()) {
			responseBase.setOptionalOutputs(optionalOutput);
		}

		responseBase.setResult(result);
		return responseBase;
	}

	@Override
	public SignResponse sign(SignRequest signRequest) {

		LOG.debug("sign");

		// parse request
		String requestId = signRequest.getRequestID();
		LOG.debug("request Id: " + requestId);

		// verify profile
		String profile = signRequest.getProfile();
		LOG.debug("signRequest.profile: " + profile);
		if (!profile.equals(DSSConstants.ARTIFACT_NAMESPACE)) {
			return DSSUtil.createRequestorSignErrorResponse(requestId,
					DSSConstants.RESULT_MINOR_NOT_SUPPORTED,
					"Profile not supported.");
		}

		// parse OptionalInput's
		boolean returnStorageInfo = false;
		String documentId = null;

		AnyType optionalInputs = signRequest.getOptionalInputs();
		if (null == optionalInputs) {
			return DSSUtil.createRequestorSignErrorResponse(requestId,
					DSSConstants.RESULT_MINOR_NOT_SUPPORTED,
					"Expecting Artifact Binding OptionalInputs...");
		}
		for (Object optionalInputObject : optionalInputs.getAny()) {

			if (optionalInputObject instanceof Element) {

				Element element = (Element) optionalInputObject;

				if (DSSConstants.ARTIFACT_NAMESPACE.equals(element
						.getNamespaceURI())
						&& DSSConstants.RETURN_STORAGE_INFO.equals(element
								.getLocalName())) {

					returnStorageInfo = true;

				} else if (DSSConstants.ARTIFACT_NAMESPACE.equals(element
						.getNamespaceURI())
						&& DSSConstants.RETURN_STORED_DOCUMENT.equals(element
								.getLocalName())) {

					try {
						ReturnStoredDocument returnStoredDocument = DSSUtil
								.getReturnStoredDocument(element);
						documentId = returnStoredDocument.getIdentifier();
					} catch (JAXBException e) {

						return DSSUtil.createRequestorSignErrorResponse(
								requestId,
								DSSConstants.RESULT_MINOR_NOT_SUPPORTED,
								"Failed to unmarshall \"ReturnStoredDocument\""
										+ " element.");
					}

				} else {

					return DSSUtil.createRequestorSignErrorResponse(
							requestId,
							DSSConstants.RESULT_MINOR_NOT_SUPPORTED,
							"Unexpected OptionalInput: \""
									+ element.getLocalName() + "\"");

				}
			}
		}

		if (!returnStorageInfo && null == documentId || returnStorageInfo
				&& null != documentId) {

			return DSSUtil.createRequestorSignErrorResponse(requestId,
					DSSConstants.RESULT_MINOR_NOT_SUPPORTED,
					"Missing Artifact Binding OptionalInputs...");
		}

		if (returnStorageInfo) {

			return storageInfoResponse(signRequest);
		} else {

			return storedDocumentResponse(signRequest, documentId);
		}
	}

	private SignResponse storedDocumentResponse(SignRequest signRequest,
			String documentId) {

		// construct response
		ObjectFactory dssObjectFactory = new ObjectFactory();

		SignResponse signResponse = dssObjectFactory.createSignResponse();
		signResponse.setRequestID(signRequest.getRequestID());
		signResponse.setProfile(DSSConstants.ARTIFACT_NAMESPACE);

		Result result = dssObjectFactory.createResult();
		signResponse.setResult(result);
		result.setResultMajor(DSSConstants.RESULT_MAJOR_SUCCESS);

		// add signed document
		DocumentEntity documentEntity = this.documentService
				.retrieve(documentId);
		if (null == documentEntity) {
			return DSSUtil.createRequestorSignErrorResponse(
					signRequest.getRequestID(),
					DSSConstants.RESULT_MINOR_NOT_SUPPORTED,
					"Document not found or expired...");
		}
		String mimeType = documentEntity.getContentType();

		DocumentWithSignature documentWithSignature = dssObjectFactory
				.createDocumentWithSignature();
		DocumentType document = dssObjectFactory.createDocumentType();
		documentWithSignature.setDocument(document);
		if (null == mimeType || "text/xml".equals(mimeType)) {
			document.setBase64XML(documentEntity.getData());
		} else {
			Base64Data base64Data = dssObjectFactory.createBase64Data();
			base64Data.setValue(documentEntity.getData());
			base64Data.setMimeType(mimeType);
			document.setBase64Data(base64Data);
		}
		AnyType optionalOutputs = dssObjectFactory.createAnyType();
		signResponse.setOptionalOutputs(optionalOutputs);
		optionalOutputs.getAny().add(documentWithSignature);

		return signResponse;
	}

	private SignResponse storageInfoResponse(SignRequest signRequest) {

		List<DocumentDO> documents = getDocuments(signRequest
				.getInputDocuments());

		if (documents.isEmpty()) {
			return DSSUtil.createRequestorSignErrorResponse(
					signRequest.getRequestID(), null,
					"No valid document found to validate.");
		}
		if (documents.size() != 1) {
			return DSSUtil.createRequestorSignErrorResponse(
					signRequest.getRequestID(), null,
					"Can validate only one document.");
		}

		// store artifact
		String documentId = UUID.randomUUID().toString();

		DateTime expiration = this.documentService.store(documentId, documents
				.get(0).getDocumentData(), documents.get(0).getContentType());

		// construct response
		ObjectFactory dssObjectFactory = new ObjectFactory();
		be.fedict.eid.dss.ws.profile.artifact.jaxb.ObjectFactory artifactObjectFactory = new be.fedict.eid.dss.ws.profile.artifact.jaxb.ObjectFactory();

		SignResponse signResponse = dssObjectFactory.createSignResponse();
		signResponse.setRequestID(signRequest.getRequestID());
		signResponse.setProfile(DSSConstants.ARTIFACT_NAMESPACE);

		Result result = dssObjectFactory.createResult();
		signResponse.setResult(result);

		result.setResultMajor(DSSConstants.RESULT_MAJOR_SUCCESS);

		AnyType optionalOutputs = dssObjectFactory.createAnyType();
		signResponse.setOptionalOutputs(optionalOutputs);

		StorageInfo storageInfo = artifactObjectFactory.createStorageInfo();
		storageInfo.setIdentifier(documentId);
		ValidityType validity = artifactObjectFactory.createValidityType();
		validity.setNotBefore(DSSUtil.toXML(new DateTime()));
		validity.setNotAfter(DSSUtil.toXML(expiration));
		storageInfo.setValidity(validity);

		optionalOutputs.getAny()
				.add(DSSUtil.getStorageInfoElement(storageInfo));

		return signResponse;
	}

	class DocumentDO {

		private final byte[] documentData;
		private final String contentType;

		public DocumentDO(byte[] documentData, String contentType) {
			this.documentData = documentData;
			this.contentType = contentType;
		}

		public byte[] getDocumentData() {
			return documentData;
		}

		public String getContentType() {
			return contentType;
		}
	}

	private List<DocumentDO> getDocuments(InputDocuments inputDocuments) {

		List<DocumentDO> documents = new LinkedList<DocumentDO>();
		List<Object> documentObjects = inputDocuments
				.getDocumentOrTransformedDataOrDocumentHash();

		for (Object documentObject : documentObjects) {

			if (!(documentObject instanceof DocumentType)) {
				continue;
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
			if (null == data || null == mimeType) {
				continue;
			}
			documents.add(new DocumentDO(data, mimeType));
		}
		return documents;
	}
}
