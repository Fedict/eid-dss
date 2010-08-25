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

package be.fedict.eid.dss.protocol.simple;

import java.security.cert.X509Certificate;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.spi.BrowserPOSTResponse;
import be.fedict.eid.dss.spi.DSSProtocolService;
import be.fedict.eid.dss.spi.DSSRequest;
import be.fedict.eid.dss.spi.SignatureStatus;

/**
 * Implementation of a very simple DSS protocol.
 * 
 * @author Frank Cornelis
 * 
 */
public class SimpleDSSProtocolService implements DSSProtocolService {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(SimpleDSSProtocolService.class);

	public static final String TARGET_PARAMETER = "target";
	public static final String SIGNATURE_REQUEST_PARAMETER = "SignatureRequest";
	public static final String LANGUAGE_PARAMETER = "language";

	public static final String TARGET_SESSION_ATTRIBUTE = SimpleDSSProtocolService.class
			.getName() + ".Target";
	public static final String SIGNATURE_REQUEST_SESSION_ATTRIBUTE = SimpleDSSProtocolService.class
			.getName() + ".SignatureRequest";

	public DSSRequest handleIncomingRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		LOG.debug("handleIncomingRequest");
		String target = request.getParameter(TARGET_PARAMETER);
		if (null == target) {
			throw new IllegalArgumentException("missing target parameter");
		}
		HttpSession httpSession = request.getSession();
		storeTarget(target, httpSession);

		String language = request.getParameter(LANGUAGE_PARAMETER);

		String signatureRequest = request
				.getParameter(SIGNATURE_REQUEST_PARAMETER);
		if (null == signatureRequest) {
			throw new IllegalArgumentException("missing parameter: "
					+ SIGNATURE_REQUEST_PARAMETER);
		}
		/*
		 * Needed during response for service signature.
		 */
		storeSignatureRequest(signatureRequest, httpSession);

		byte[] decodedSignatureRequest = Base64.decodeBase64(signatureRequest);

		DSSRequest dssRequest = new DSSRequest(decodedSignatureRequest,
				"text/xml", language);

		return dssRequest;
	}

	private void storeTarget(String target, HttpSession httpSession) {
		httpSession.setAttribute(TARGET_SESSION_ATTRIBUTE, target);
	}

	private String retrieveTarget(HttpSession httpSession) {
		String target = (String) httpSession
				.getAttribute(TARGET_SESSION_ATTRIBUTE);
		return target;
	}

	private void storeSignatureRequest(String signatureRequest,
			HttpSession httpSession) {
		httpSession.setAttribute(SIGNATURE_REQUEST_SESSION_ATTRIBUTE,
				signatureRequest);
	}

	public BrowserPOSTResponse handleResponse(SignatureStatus signatureStatus,
			byte[] signedDocument, X509Certificate signerCertificate,
			HttpSession httpSession, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		LOG.debug("handleResponse");
		String target = retrieveTarget(httpSession);
		BrowserPOSTResponse browserPOSTResponse = new BrowserPOSTResponse(
				target);
		browserPOSTResponse.addAttribute("SignatureStatus",
				signatureStatus.getStatus());
		if (SignatureStatus.OK == signatureStatus) {
			String encodedSignedDocument = Base64
					.encodeBase64String(signedDocument);
			browserPOSTResponse.addAttribute("SignatureResponse",
					encodedSignedDocument);
			byte[] derSignerCertificate = signerCertificate.getEncoded();
			String encodedSignatureCertificate = Base64
					.encodeBase64String(derSignerCertificate);
			browserPOSTResponse.addAttribute("SignatureCertificate",
					encodedSignatureCertificate);
		}
		return browserPOSTResponse;
	}

	public void init(ServletContext servletContext) {
		LOG.debug("init");
	}
}
