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

import javax.annotation.PostConstruct;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.servlet.http.HttpSession;

import org.bouncycastle.util.encoders.Base64;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;

import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;

@Stateful
@Name("xmlResponse")
@LocalBinding(jndiBinding = "fedict/eid/dss/XMLResponseBean")
public class XMLResponseBean implements XMLResponse {

	@Logger
	private Log log;

	private String encodedSignatureResponse;

	private String target;

	@Remove
	@Destroy
	public void destroy() {
		this.log.debug("destroy");
	}

	@PostConstruct
	public void postConstruct() {
		this.log.debug("postConstruct");
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		this.target = documentRepository.getTarget();
		this.log.debug("target: " + target);
		String signedDocument = documentRepository.getSignedDocument();
		this.encodedSignatureResponse = new String(Base64.encode(signedDocument
				.getBytes()));
	}

	public String getEncodedSignatureResponse() {
		return this.encodedSignatureResponse;
	}

	public String getTarget() {
		return this.target;
	}

	public void setEncodedSignatureResponse(String encodedSignatureResponse) {
		this.encodedSignatureResponse = encodedSignatureResponse;
	}

	public void setTarget(String target) {
		this.target = target;
	}
}
