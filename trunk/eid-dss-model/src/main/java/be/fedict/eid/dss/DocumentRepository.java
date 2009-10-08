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

package be.fedict.eid.dss;

import javax.servlet.http.HttpSession;

/**
 * A document repository based on HTTP session storage.
 * 
 * @author fcorneli
 * 
 */
public class DocumentRepository {

	public static final String DOCUMENT_SESSION_ATTRIBUTE = DocumentRepository.class
			.getName()
			+ ".document";

	public static final String SIGNED_DOCUMENT_SESSION_ATTRIBUTE = DocumentRepository.class
			.getName()
			+ ".signedDocument";

	public static final String TARGET_SESSION_ATTRIBUTE = DocumentRepository.class
			.getName()
			+ ".target";

	public final HttpSession httpSession;

	public DocumentRepository(HttpSession httpSession) {
		this.httpSession = httpSession;
	}

	public void setDocument(String document) {
		this.httpSession.setAttribute(DOCUMENT_SESSION_ATTRIBUTE, document);
	}

	public String getDocument() {
		String document = (String) this.httpSession
				.getAttribute(DOCUMENT_SESSION_ATTRIBUTE);
		return document;
	}

	public void setSignedDocument(String signedDocument) {
		this.httpSession.setAttribute(SIGNED_DOCUMENT_SESSION_ATTRIBUTE,
				signedDocument);
	}

	public String getSignedDocument() {
		String signedDocument = (String) this.httpSession
				.getAttribute(SIGNED_DOCUMENT_SESSION_ATTRIBUTE);
		return signedDocument;
	}

	public void setTarget(String target) {
		this.httpSession.setAttribute(TARGET_SESSION_ATTRIBUTE, target);
	}

	public String getTarget() {
		String target = (String) this.httpSession
				.getAttribute(TARGET_SESSION_ATTRIBUTE);
		return target;
	}
}
