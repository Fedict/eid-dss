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

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import javax.ejb.Stateless;
import javax.servlet.http.HttpSession;

import org.bouncycastle.util.encoders.Base64;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;

import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;

/**
 * XML Response bean. Used by the post-response.xhtml page.
 * 
 * @author fcorneli
 * 
 */
@Stateless
@Name("xmlResponse")
@LocalBinding(jndiBinding = "fedict/eid/dss/XMLResponseBean")
public class XMLResponseBean implements XMLResponse {

	@Logger
	private Log log;

	private String encodedSignatureResponse;

	private String target;

	private String signatureStatus;

	private String encodedSignatureCertificate;

	public String getEncodedSignatureResponse() {
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);

		String signedDocument = documentRepository.getSignedDocument();
		if (null != signedDocument) {
			this.encodedSignatureResponse = new String(Base64
					.encode(signedDocument.getBytes()));
		}

		return this.encodedSignatureResponse;
	}

	public String getTarget() {
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		this.target = documentRepository.getTarget();
		this.log.debug("target: " + target);
		return this.target;
	}

	public void setEncodedSignatureResponse(String encodedSignatureResponse) {
		this.encodedSignatureResponse = encodedSignatureResponse;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getSignatureStatus() {
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		this.signatureStatus = documentRepository.getSignatureStatus()
				.getStatus();
		this.log.debug("signature status: " + this.signatureStatus);
		return this.signatureStatus;
	}

	public void setSignatureStatus(String signatureStatus) {
		this.signatureStatus = signatureStatus;
	}

	public String getEncodedSignatureCertificate() {
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);

		X509Certificate signerCertificate = documentRepository
				.getSignerCertificate();
		if (null != signerCertificate) {
			try {
				this.encodedSignatureCertificate = new String(Base64
						.encode(signerCertificate.getEncoded()));
			} catch (CertificateEncodingException e) {
				this.log.error("certificate encoding error: " + e.getMessage(),
						e);
			}
		} else {
			this.log.error("signer certificate is null");
		}
		return this.encodedSignatureCertificate;
	}

	public void setEncodedSignatureCertificate(
			String encodedSignatureCertificate) {
		this.encodedSignatureCertificate = encodedSignatureCertificate;
	}
}
