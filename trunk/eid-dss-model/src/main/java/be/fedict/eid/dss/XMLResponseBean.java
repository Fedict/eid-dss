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
 * @author Frank Cornelis
 * 
 */
@Stateless
@Name("xmlResponse")
@LocalBinding(jndiBinding = "fedict/eid/dss/XMLResponseBean")
public class XMLResponseBean implements XMLResponse {

	@Logger
	private Log log;

	public String getEncodedSignatureResponse() {
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);

		String signedDocument = documentRepository.getSignedDocument();
		if (null != signedDocument) {
			return new String(Base64.encode(signedDocument.getBytes()));
		}

		return null;
	}

	public String getTarget() {
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		String target = documentRepository.getTarget();
		this.log.debug("target: " + target);
		return target;
	}

	public void setEncodedSignatureResponse(String encodedSignatureResponse) {
		// keep JSF happy
	}

	public void setTarget(String target) {
		// keep JSF happy
	}

	public String getSignatureStatus() {
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		String signatureStatus = documentRepository.getSignatureStatus()
				.getStatus();
		this.log.debug("signature status: " + signatureStatus);
		return signatureStatus;
	}

	public void setSignatureStatus(String signatureStatus) {
		// keep JSF happy
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
				return new String(Base64.encode(signerCertificate.getEncoded()));
			} catch (CertificateEncodingException e) {
				this.log.error("certificate encoding error: " + e.getMessage(),
						e);
			}
		} else {
			this.log.error("signer certificate is null");
		}
		return null;
	}

	public void setEncodedSignatureCertificate(
			String encodedSignatureCertificate) {
		// keep JSF happy
	}
}
