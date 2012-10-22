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
import java.io.OutputStream;

import javax.ejb.EJB;
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
import org.jboss.seam.contexts.SessionContext;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.log.Log;

import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;
import be.fedict.eid.dss.control.View;
import be.fedict.eid.dss.entity.RPEntity;
import be.fedict.eid.dss.model.DocumentRepository;
import be.fedict.eid.dss.model.MailManager;
import be.fedict.eid.dss.spi.SignatureStatus;

@Stateful
@Name("dssView")
@LocalBinding(jndiBinding = "fedict/eid/dss/ViewBean")
public class ViewBean implements View {

	@Logger
	private Log log;

	@In(create = true)
	private SessionContext sessionContext;

	@In
	private LocaleSelector localeSelector;

	@In(value = View.LANGUAGE_SESSION_ATTRIBUTE, scope = ScopeType.SESSION, required = false)
	private String language;

	private String role;

	private boolean includeIdentity;

	private String email;

	@EJB
	private MailManager mailManager;

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
		documentRepository.setEmail(this.email);
		documentRepository.setIncludeIdentity(this.includeIdentity);
		return "sign";
	}

	@Override
	public boolean getIncludeIdentity() {
		return this.includeIdentity;
	}

	@Override
	public void setIncludeIdentity(boolean includeIdentity) {
		this.includeIdentity = includeIdentity;
	}

	@Override
	public String getRp() {

		RPEntity rp = (RPEntity) this.sessionContext.get(RP_SESSION_ATTRIBUTE);
		if (null != rp) {
			return rp.getName();
		}
		return null;
	}

	@Override
	public boolean isRpLogo() {

		RPEntity rp = (RPEntity) this.sessionContext.get(RP_SESSION_ATTRIBUTE);
		return null != rp && null != rp.getLogo();

	}

	@Override
	public void paint(OutputStream stream, Object object) throws IOException {

		RPEntity rp = (RPEntity) this.sessionContext.get(RP_SESSION_ATTRIBUTE);

		if (null != rp && null != rp.getLogo()) {
			this.log.debug("paint logo");
			stream.write(rp.getLogo());
			stream.close();
		}
	}

	@Override
	public long getTimeStamp() {
		return System.currentTimeMillis();
	}

	@Override
	public String getEmail() {
		return this.email;
	}

	@Override
	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public boolean getDisplayMailSignedDocument() {
		return this.mailManager.sendSignedDocumentEnabled();
	}

	@Override
	public boolean isDisableButtons() {
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		return null == documentRepository.getDocument();
	}
}
