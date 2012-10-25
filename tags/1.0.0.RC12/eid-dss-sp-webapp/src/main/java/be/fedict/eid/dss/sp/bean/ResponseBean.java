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

import java.util.List;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.client.DigitalSignatureServiceClient;
import be.fedict.eid.dss.client.NotParseableXMLDocumentException;
import be.fedict.eid.dss.client.SignatureInfo;

public class ResponseBean {

	private static final Log LOG = LogFactory.getLog(ResponseBean.class);

	private ServletRequest request;

	private String error;
	private byte[] signedDocument;
	private String contentType;
	private List<SignatureInfo> signatureInfos = null;

	public ServletRequest getRequest() {
		return this.request;
	}

	public void setRequest(ServletRequest request) {

		this.request = request;
		HttpServletRequest httpServletRequest = (HttpServletRequest) request;

		this.error = (String) httpServletRequest.getSession().getAttribute(
				"SignatureErrorMessage");
		this.signedDocument = (byte[]) httpServletRequest.getSession()
				.getAttribute("document");
		this.contentType = (String) httpServletRequest.getSession()
				.getAttribute("ContentType");

		if (null != this.signedDocument) {

			DigitalSignatureServiceClient dssClient = new DigitalSignatureServiceClient();
			dssClient.setLogging(true, false);

			try {
				LOG.debug("verify signed document");
				signatureInfos = dssClient.verifyWithSigners(
						this.signedDocument, this.contentType);

				for (SignatureInfo signatureInfo : signatureInfos) {
					LOG.debug("SignatureInfo: "
							+ signatureInfo.getSigner().toString() + " time="
							+ signatureInfo.getSigningTime() + " role="
							+ signatureInfo.getRole());
				}

			} catch (NotParseableXMLDocumentException e) {
				throw new RuntimeException(e);
			}
		}
	}

	public String getError() {
		return this.error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public List<SignatureInfo> getSignatureInfos() {
		return this.signatureInfos;
	}

	public void setSignatureInfos(List<SignatureInfo> signatureInfos) {
		this.signatureInfos = signatureInfos;
	}
}
