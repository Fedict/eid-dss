/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010 FedICT.
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

import be.fedict.eid.dss.client.DigitalSignatureServiceClient;
import be.fedict.eid.dss.client.StorageInfoDO;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.SignatureVerificationService;
import be.fedict.eid.dss.model.exception.DocumentFormatException;
import be.fedict.eid.dss.model.exception.InvalidSignatureException;
import be.fedict.eid.dss.portal.control.View;
import be.fedict.eid.dss.spi.SignatureInfo;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.log.Log;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import java.util.List;
import java.util.UUID;

@Stateful
@Name("dssPortalView")
@LocalBinding(jndiBinding = "fedict/eid/dss/portal/ViewBean")
public class ViewBean implements View {

	@Logger
	private Log log;

	@EJB
	private SignatureVerificationService signatureVerificationService;

	@EJB
	private Configuration configuration;

	@In(value = "document", scope = ScopeType.SESSION, required = true)
	private byte[] document;

	@Out(value = "target", scope = ScopeType.SESSION, required = false)
	private String target;

	@Out(value = "SignatureRequestId", scope = ScopeType.SESSION, required = false)
	private String signatureRequestId;

	@Out(value = "language", scope = ScopeType.SESSION, required = false)
	private String language;

	@Out(value = "RelayState", scope = ScopeType.SESSION, required = false)
	private String relayState;

	@In(value = "filesize", scope = ScopeType.SESSION, required = false)
	@Out(value = "filesize", scope = ScopeType.SESSION, required = false)
	private Integer filesize;

	@In(value = "ContentType", scope = ScopeType.SESSION, required = false)
	private String contentType;

	@DataModel
	private List<SignatureInfo> signatureInfos;

	@In
	private LocaleSelector localeSelector;

	@Remove
	@Destroy
	@Override
	public void destroy() {
		this.log.debug("destroy");
	}

	@Override
	public void verifySignatures() {
		this.filesize = this.document.length;
		try {
			this.signatureInfos = this.signatureVerificationService.verify(
					this.document, this.contentType, null);
		} catch (DocumentFormatException e) {
			this.log.error("document format error: #0", e.getMessage());
			return;
		} catch (InvalidSignatureException e) {
			this.log.error("invalid signature: #0", e.getMessage());
			return;
		}
		this.log.debug("number of signatures: #0", this.signatureInfos.size());
		for (SignatureInfo signatureInfo : this.signatureInfos) {
			this.log.debug("signer: #0", signatureInfo.getSigner()
					.getSubjectX500Principal());
			this.log.debug("signing time: #0", signatureInfo.getSigningTime());
		}
	}

	@Override
	public void prepareForSigning() {

		this.log.debug("prepare for signing");

		// store document via WS
		String dssWSUrl = this.configuration.getValue(
				ConfigProperty.DSS_WS_URL, String.class);
		DigitalSignatureServiceClient dssClient = new DigitalSignatureServiceClient(
				dssWSUrl);
		dssClient.setLogging(true, false);
		StorageInfoDO storageInfoDO = dssClient.store(document,
				this.contentType);

		this.signatureRequestId = storageInfoDO.getArtifact();
		this.log.debug("DSS Artifact ID: " + this.signatureRequestId);

		FacesContext facesContext = FacesContext.getCurrentInstance();
		ExternalContext externalContext = facesContext.getExternalContext();
		String requestContextPath = externalContext.getRequestContextPath();
		this.target = requestContextPath + "/dss-response";

		this.language = this.localeSelector.getLanguage();
		this.relayState = UUID.randomUUID().toString();
		this.log.debug("RelayState: " + relayState);

	}
}
