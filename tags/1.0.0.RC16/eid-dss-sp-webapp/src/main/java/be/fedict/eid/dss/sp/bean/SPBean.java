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

package be.fedict.eid.dss.sp.bean;

import java.util.Random;
import java.util.UUID;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;

import be.fedict.eid.dss.client.DigitalSignatureServiceClient;
import be.fedict.eid.dss.client.StorageInfoDO;
import be.fedict.eid.dss.protocol.simple.client.ServiceSignatureDO;
import be.fedict.eid.dss.protocol.simple.client.SignatureRequestUtil;
import be.fedict.eid.dss.sp.servlet.PkiServlet;
import be.fedict.eid.dss.sp.servlet.UploadServlet;

public class SPBean {

	private static final Log LOG = LogFactory.getLog(SPBean.class);

	private ServletRequest request;

	private String signatureRequest;
	private String signatureRequestId;
	private String contentType;

	private String relayState;
	private String language;

	private String destination;
	private String target;

	private ServiceSignatureDO serviceSignature;

	public ServletRequest getPostRequest() {
		return this.request;
	}

	public ServletRequest getArtifactRequest() {
		return this.request;
	}

	public ServletRequest getArtifactRequestSigned() {
		return this.request;
	}

	public void setPostRequest(ServletRequest request) {

		this.request = request;
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;

		byte[] document = setRequest(httpServletRequest, "en", "dss-response");

		if (null != document) {

			this.signatureRequest = new String(Base64.encode(document));
			httpServletRequest.getSession().setAttribute("SignatureRequest",
					this.signatureRequest);
			httpServletRequest.getSession().setAttribute("ContentType",
					this.contentType);
		}
	}

	public void setArtifactRequest(ServletRequest request) {

		this.request = request;
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;

		byte[] document = setRequest(httpServletRequest, "nl",
				"dss-response-artifact");

		if (null != document) {

			// SignRequest to DSS WS
			DigitalSignatureServiceClient dssClient = new DigitalSignatureServiceClient();
			dssClient.setLogging(true, false);

			StorageInfoDO storageInfoDO = dssClient.store(document,
					this.contentType);

			LOG.debug("StorageInfo.notBefore: " + storageInfoDO.getNotBefore());
			LOG.debug("StorageInfo.notAfter: " + storageInfoDO.getNotAfter());

			this.signatureRequestId = storageInfoDO.getArtifact();
			httpServletRequest.getSession().setAttribute("SignatureRequestId",
					this.signatureRequestId);
		}
	}

	public void setArtifactRequestSigned(ServletRequest request) {

		this.request = request;
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;

		byte[] document = setRequest(httpServletRequest, "nl",
				"dss-response-artifact");

		if (null != document) {

			// SignRequest to DSS WS
			DigitalSignatureServiceClient dssClient = new DigitalSignatureServiceClient();
			dssClient.setLogging(true, false);

			StorageInfoDO storageInfoDO = dssClient.store(document,
					this.contentType);

			LOG.debug("StorageInfo.notBefore: " + storageInfoDO.getNotBefore());
			LOG.debug("StorageInfo.notAfter: " + storageInfoDO.getNotAfter());

			this.signatureRequestId = storageInfoDO.getArtifact();
			httpServletRequest.getSession().setAttribute("SignatureRequestId",
					this.signatureRequestId);

			try {
				this.serviceSignature = SignatureRequestUtil
						.getServiceSignature(PkiServlet.getPrivateKeyEntry(),
								null, this.signatureRequestId, this.target,
								this.language, null, this.relayState);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}

	private byte[] setRequest(HttpServletRequest httpServletRequest,
			String language, String target) {

		byte[] document = (byte[]) httpServletRequest.getSession()
				.getAttribute(UploadServlet.DOCUMENT_SESSION_ATTRIBUTE);

		this.language = language;
		this.contentType = (String) httpServletRequest.getSession()
				.getAttribute("ContentType");
		this.relayState = UUID.randomUUID().toString();
		LOG.debug("RelayState: " + this.relayState);

		this.destination = "../eid-dss/protocol/simple";
		this.target = httpServletRequest.getScheme() + "://"
				+ httpServletRequest.getServerName() + ":"
				+ httpServletRequest.getServerPort()
				+ httpServletRequest.getContextPath() + "/" + target
				+ "?requestId=" + new Random().nextInt(1000);

		// store data on session for response handling
		httpServletRequest.getSession().setAttribute("target", this.target);
		httpServletRequest.getSession().setAttribute("RelayState",
				this.relayState);

		return document;
	}

	public String getSignatureRequest() {
		return this.signatureRequest;
	}

	public void setSignatureRequest(String signatureRequest) {
		this.signatureRequest = signatureRequest;
	}

	public String getContentType() {
		return this.contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getRelayState() {
		return this.relayState;
	}

	public void setRelayState(String relayState) {
		this.relayState = relayState;
	}

	public String getDestination() {
		return this.destination;
	}

	public void setDestination(String destination) {
		this.destination = destination;
	}

	public String getTarget() {
		return this.target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getSignatureRequestId() {
		return signatureRequestId;
	}

	public void setSignatureRequestId(String signatureRequestId) {
		this.signatureRequestId = signatureRequestId;
	}

	/*
	 * Service Signature getters
	 */

	public String getServiceSigned() {
		if (null != this.serviceSignature) {
			return this.serviceSignature.getServiceSigned();
		}
		return null;
	}

	public String getServiceSignature() {
		if (null != this.serviceSignature) {
			return this.serviceSignature.getServiceSignature();
		}
		return null;
	}

	public String getServiceCertificateChainSize() {
		if (null != this.serviceSignature) {
			return this.serviceSignature.getServiceCertificateChainSize();
		}
		return null;
	}

	public String getServiceCertificate() {
		if (null != this.serviceSignature) {
			return this.serviceSignature.getServiceCertificates().get(0);
		}
		return null;
	}
}
