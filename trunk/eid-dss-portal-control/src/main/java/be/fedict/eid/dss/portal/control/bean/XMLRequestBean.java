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

package be.fedict.eid.dss.portal.control.bean;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import org.bouncycastle.util.encoders.Base64;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.log.Log;

import be.fedict.eid.dss.portal.control.XMLRequest;

@Stateful
@Name("xmlRequest")
@LocalBinding(jndiBinding = "fedict/eid/dss/portal/XMLRequestBean")
public class XMLRequestBean implements XMLRequest {

	@Logger
	private Log log;

	@In
	private LocaleSelector localeSelector;

	private String encodedDocument;

	private String document;

	private String target;

	@Out(value = "target", scope = ScopeType.SESSION, required = false)
	private String sessionTarget;

	@Out(value = "SignatureRequest", scope = ScopeType.SESSION, required = false)
	private String signatureRequest;

	@Remove
	@Destroy
	public void destroy() {
		this.log.debug("destroy");
	}

	public String getDocument() {
		return this.document;
	}

	public void setDocument(String document) {
		this.document = document;
	}

	public String getEncodedDocument() {
		return this.encodedDocument;
	}

	public void setEncodedDocument(String encodedDocument) {
		this.encodedDocument = encodedDocument;
	}

	public String submit() {
		this.log.debug("submit");
		this.encodedDocument = new String(Base64.encode(this.document
				.getBytes()));
		this.signatureRequest = this.encodedDocument;

		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		String requestContextPath = externalContext.getRequestContextPath();
		this.target = requestContextPath + "/post-response";
		this.sessionTarget = this.target;

		return "success";
	}

	@Override
	public String getLanguage() {
		String language = this.localeSelector.getLanguage();
		this.log.debug("language: #0", language);
		return language;
	}

	@Override
	public void setLanguage(String language) {
	}

	@Override
	public String getTarget() {
		return this.target;
	}

	@Override
	public void setTarget(String target) {
	}
}
