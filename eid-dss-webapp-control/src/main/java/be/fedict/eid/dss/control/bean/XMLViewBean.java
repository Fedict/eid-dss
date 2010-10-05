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

package be.fedict.eid.dss.control.bean;

import java.io.IOException;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.log.Log;

import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;
import be.fedict.eid.dss.control.XMLView;
import be.fedict.eid.dss.model.DocumentRepository;
import be.fedict.eid.dss.spi.SignatureStatus;

@Stateful
@Name("xmlView")
@LocalBinding(jndiBinding = "fedict/eid/dss/XMLViewBean")
public class XMLViewBean implements XMLView {

	@Logger
	private Log log;

	@In
	private LocaleSelector localeSelector;

	@In(value = XMLView.LANGUAGE_SESSION_ATTRIBUTE, scope = ScopeType.SESSION)
	private String language;

	private String role;

	public String cancel() {
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		documentRepository.setSignatureStatus(SignatureStatus.USER_CANCELLED);

		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		HttpServletResponse httpServletResponse = (HttpServletResponse) externalContext
				.getResponse();
		HttpServletRequest httpServletRequest = (HttpServletRequest) externalContext
				.getRequest();
		String redirectUrl = httpServletRequest.getContextPath()
				+ "/protocol-exit";
		try {
			httpServletResponse.sendRedirect(redirectUrl);
		} catch (IOException e) {
			this.log.error("I/O error: #0", e, e.getMessage());
		}
		return null;
	}

	@Override
	public void initLanguage() {
		this.log.debug("language: #0", this.language);
		if (null != this.language) {
			this.localeSelector.setLocaleString(language);
			this.localeSelector.select();
		}
	}

	@Override
	public String getRole() {
		return this.role;
	}

	@Override
	public void setRole(String role) {
		this.role = role;
	}

	@Remove
	@Destroy
	@Override
	public void destroy() {
		this.log.debug("destroy");
	}

	@Override
	public String sign() {
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		documentRepository.setRole(this.role);
		return "sign";
	}
}
