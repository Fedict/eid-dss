/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009 FedICT.
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

package be.fedict.eid.dss.model;

import java.security.cert.X509Certificate;

import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.spi.SignatureStatus;

/**
 * A document repository based on HTTP session storage.
 * 
 * @author Frank Cornelis
 */
public class DocumentRepository {

	private static final Log LOG = LogFactory.getLog(DocumentRepository.class);

	public static final String DOCUMENT_SESSION_ATTRIBUTE = DocumentRepository.class
			.getName() + ".document";

	public static final String DOCUMENT_CONTENT_TYPE_SESSION_ATTRIBUTE = DocumentRepository.class
			.getName() + ".DocumentContentType";

	public static final String DOCUMENT_ID_SESSION_ATTRIBUTE = DocumentRepository.class
			.getName() + ".DocumentId";

	public static final String SIGNED_DOCUMENT_SESSION_ATTRIBUTE = DocumentRepository.class
			.getName() + ".signedDocument";

	public static final String TARGET_SESSION_ATTRIBUTE = DocumentRepository.class
			.getName() + ".target";

	public static final String SIGNATURE_STATUS_SESSION_ATTRIBUTE = DocumentRepository.class
			.getName() + ".status";

	public static final String SIGNER_CERTIFICATE_SESSION_ATTRIBUTE = DocumentRepository.class
			.getName() + ".certificate";

	public static final String ROLE_SESSION_ATTRIBUTE = DocumentRepository.class
			.getName() + ".role";

	public static final String INCLUDE_IDENTITY_SESSION_ATTRIBUTE = DocumentRepository.class
			.getName() + ".includeIdentity";

	public static final String EMAIL_SESSION_ATTRIBUTE = DocumentRepository.class
			.getName() + ".email";

	public final HttpSession httpSession;

	public DocumentRepository(HttpSession httpSession) {
		this.httpSession = httpSession;
	}

	public void reset() {
		this.httpSession.removeAttribute(DOCUMENT_SESSION_ATTRIBUTE);
		this.httpSession
				.removeAttribute(DOCUMENT_CONTENT_TYPE_SESSION_ATTRIBUTE);
		this.httpSession.removeAttribute(DOCUMENT_ID_SESSION_ATTRIBUTE);
		this.httpSession.removeAttribute(SIGNED_DOCUMENT_SESSION_ATTRIBUTE);
		this.httpSession.removeAttribute(TARGET_SESSION_ATTRIBUTE);
		this.httpSession.removeAttribute(SIGNATURE_STATUS_SESSION_ATTRIBUTE);
		this.httpSession.removeAttribute(SIGNER_CERTIFICATE_SESSION_ATTRIBUTE);
		this.httpSession.removeAttribute(ROLE_SESSION_ATTRIBUTE);
		this.httpSession.removeAttribute(INCLUDE_IDENTITY_SESSION_ATTRIBUTE);
		this.httpSession.removeAttribute(EMAIL_SESSION_ATTRIBUTE);
	}

	public void setDocument(byte[] document) {
		this.httpSession.setAttribute(DOCUMENT_SESSION_ATTRIBUTE, document);
	}

	public byte[] getDocument() {
		return (byte[]) this.httpSession
				.getAttribute(DOCUMENT_SESSION_ATTRIBUTE);
	}

	public void setDocumentContentType(String contentType) {
		LOG.debug("set document content type: " + contentType);
		this.httpSession.setAttribute(DOCUMENT_CONTENT_TYPE_SESSION_ATTRIBUTE,
				contentType);
	}

	public String getDocumentContentType() {
		return (String) this.httpSession
				.getAttribute(DOCUMENT_CONTENT_TYPE_SESSION_ATTRIBUTE);
	}

	public void setDocumentId(String documentId) {
		this.httpSession
				.setAttribute(DOCUMENT_ID_SESSION_ATTRIBUTE, documentId);
	}

	public String getDocumentId() {
		return (String) this.httpSession
				.getAttribute(DOCUMENT_ID_SESSION_ATTRIBUTE);
	}

	public void setSignedDocument(byte[] signedDocument) {
		this.httpSession.setAttribute(SIGNED_DOCUMENT_SESSION_ATTRIBUTE,
				signedDocument);
	}

	public byte[] getSignedDocument() {
		return (byte[]) this.httpSession
				.getAttribute(SIGNED_DOCUMENT_SESSION_ATTRIBUTE);
	}

	public void setTarget(String target) {
		this.httpSession.setAttribute(TARGET_SESSION_ATTRIBUTE, target);
	}

	public String getTarget() {
		return (String) this.httpSession.getAttribute(TARGET_SESSION_ATTRIBUTE);
	}

	public void setSignatureStatus(SignatureStatus signatureStatus) {
		this.httpSession.setAttribute(SIGNATURE_STATUS_SESSION_ATTRIBUTE,
				signatureStatus);
	}

	public SignatureStatus getSignatureStatus() {
		return (SignatureStatus) this.httpSession
				.getAttribute(SIGNATURE_STATUS_SESSION_ATTRIBUTE);
	}

	public void setSignerCertificate(X509Certificate signerCertificate) {
		this.httpSession.setAttribute(SIGNER_CERTIFICATE_SESSION_ATTRIBUTE,
				signerCertificate);
	}

	public X509Certificate getSignerCertificate() {
		return (X509Certificate) this.httpSession
				.getAttribute(SIGNER_CERTIFICATE_SESSION_ATTRIBUTE);
	}

	public void setRole(String role) {
		this.httpSession.setAttribute(ROLE_SESSION_ATTRIBUTE, role);
	}

	public String getRole() {
		return (String) this.httpSession.getAttribute(ROLE_SESSION_ATTRIBUTE);
	}

	public void setIncludeIdentity(boolean includeIdentity) {
		this.httpSession.setAttribute(INCLUDE_IDENTITY_SESSION_ATTRIBUTE,
				includeIdentity);
	}

	public boolean getIncludeIdentity() {
		return (Boolean) this.httpSession
				.getAttribute(INCLUDE_IDENTITY_SESSION_ATTRIBUTE);
	}

	public void setEmail(String email) {
		this.httpSession.setAttribute(EMAIL_SESSION_ATTRIBUTE, email);
	}

	public String getEmail() {
		return (String) this.httpSession.getAttribute(EMAIL_SESSION_ATTRIBUTE);
	}
}
