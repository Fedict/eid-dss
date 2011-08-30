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

package be.fedict.eid.dss.spi;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Represents a DSS request after unmarshalling by the DSSProtocolService.
 * 
 * @author Frank Cornelis
 */
public class DSSRequest implements Serializable {

	private static final long serialVersionUID = 1L;

	private final byte[] documentData;
	private final String contentType;

	private final String documentId;

	private final String language;

	private final String domain;
	private final List<X509Certificate> serviceCertificateChain;

	/**
	 * Main constructor.
	 * 
	 * @param documentData
	 *            document data, if <code>null</code> documentId needs to be
	 *            specified
	 * @param contentType
	 *            document content type, if <code>null</code> documentId needs
	 *            to be specified
	 * @param documentId
	 *            document's ID, if <code>null</code> documentData needs to be
	 *            specified
	 * @param language
	 *            optional language
	 * @param domain
	 *            domain, used for identifying a specific RP.
	 * @param serviceCertificateChain
	 *            optional service certificate chain case DSS request was signed
	 */
	public DSSRequest(byte[] documentData, String contentType,
			String documentId, String language, String domain,
			List<X509Certificate> serviceCertificateChain) {

		this.documentData = documentData;
		this.contentType = contentType;
		this.documentId = documentId;
		this.language = language;
		this.domain = domain;
		this.serviceCertificateChain = serviceCertificateChain;
	}

	/**
	 * @return the data bytes of the document to be signed.
	 */
	public byte[] getDocumentData() {
		return this.documentData;
	}

	/**
	 * @return the content type of the document to be signed.
	 */
	public String getContentType() {
		return this.contentType;
	}

	/**
	 * @return the language to be used on the GUI for signing the document.
	 */
	public String getLanguage() {
		return this.language;
	}

	/**
	 * @return the document's ID.
	 */
	public String getDocumentId() {
		return this.documentId;
	}

	/**
	 * @return the RP domain
	 */
	public String getDomain() {
		return this.domain;
	}

	/**
	 * @return the RP service certificate chain, case the DSS request was
	 *         signed.
	 */
	public List<X509Certificate> getServiceCertificateChain() {
		return this.serviceCertificateChain;
	}
}
