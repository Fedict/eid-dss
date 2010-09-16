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

package be.fedict.eid.dss.protocol.simple.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;

/**
 * Processes the response from the eID DSS simple protocol.
 * 
 * <p>
 * The following init-params are required:
 * </p>
 * <ul>
 * <li><tt>NextPage</tt>: indicates the page where the flow continues.</li>
 * <li><tt>SignedDocumentSessionAttribute</tt>: indicates which session
 * attribute to use to push in the signed document as returned by the eID DSS.</li>
 * <li><tt>ErrorPage</tt>: indicates the page to be shown in case of errors.</li>
 * <li><tt>ErrorMessageSessionAttribute</tt>: indicates which session attribute
 * to use for reporting an error. This session attribute can be used on the
 * error page.</li>
 * </ul>
 * 
 * <p>
 * The following init-params are optional:
 * </p>
 * <ul>
 * <li><tt>SignatureCertificateSessionAttribute</tt>: indicates which session
 * attribute to use to push in the signature certificate as returned by the eID
 * DSS.</li>
 * <li><tt>ServiceFingerprint</tt>: contains the hexadecimal encoded SHA1
 * fingerprint of the service certificate used to sign the DSS response. Use
 * this parameter when a very simple trust model is sufficient.</li>
 * </ul>
 * 
 * @author Frank Cornelis
 * 
 */
public class SignatureResponseProcessorServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(SignatureResponseProcessorServlet.class);

	public static final String SIGNATURE_RESPONSE_PARAMETER = "SignatureResponse";

	public static final String SIGNATURE_STATUS_PARAMETER = "SignatureStatus";

	public static final String SIGNATURE_CERTIFICATE_PARAMETER = "SignatureCertificate";

	public static final String SERVICE_SIGNED_PARAMETER = "ServiceSigned";

	public static final String SERVICE_SIGNATURE_PARAMETER = "ServiceSignature";

	public static final String SERVICE_CERTIFICATE_CHAIN_SIZE_PARAMETER = "ServiceCertificateChainSize";

	public static final String SERVICE_CERTIFICATE_PARAMETER_PREFIX = "ServiceCertificate.";

	public static final String NEXT_PAGE_INIT_PARAM = "NextPage";

	public static final String ERROR_PAGE_INIT_PARAM = "ErrorPage";

	public static final String ERROR_MESSAGE_SESSION_ATTRIBUTE_INIT_PARAM = "ErrorMessageSessionAttribute";

	public static final String SIGNED_DOCUMENT_SESSION_ATTRIBUTE_INIT_PARAM = "SignedDocumentSessionAttribute";

	public static final String SIGNATURE_CERTIFICATE_SESSION_ATTRIBUTE_INIT_PARAM = "SignatureCertificateSessionAttribute";

	public static final String SERVICE_FINGERPRINT_INIT_PARAM = "ServiceFingerprint";

	private String nextPage;

	private String errorPage;

	private String errorMessageSessionAttribute;

	private String signedDocumentSessionAttribute;

	private String signatureCertificateSessionAttribute;

	private byte[] serviceFingerprint;

	@Override
	public void init(ServletConfig config) throws ServletException {
		LOG.debug("init");
		this.nextPage = getRequiredInitParameter(config, NEXT_PAGE_INIT_PARAM);
		LOG.debug("next page: " + this.nextPage);

		this.errorPage = getRequiredInitParameter(config, ERROR_PAGE_INIT_PARAM);
		LOG.debug("error page: " + this.errorPage);

		this.errorMessageSessionAttribute = getRequiredInitParameter(config,
				ERROR_MESSAGE_SESSION_ATTRIBUTE_INIT_PARAM);
		LOG.debug("error message session attribute: "
				+ this.errorMessageSessionAttribute);

		this.signedDocumentSessionAttribute = getRequiredInitParameter(config,
				SIGNED_DOCUMENT_SESSION_ATTRIBUTE_INIT_PARAM);
		LOG.debug("signed document session attribute: "
				+ this.signedDocumentSessionAttribute);

		this.signatureCertificateSessionAttribute = config
				.getInitParameter(SIGNATURE_CERTIFICATE_SESSION_ATTRIBUTE_INIT_PARAM);

		String encodedServiceFingerprint = config
				.getInitParameter(SERVICE_FINGERPRINT_INIT_PARAM);
		if (null != encodedServiceFingerprint) {
			LOG.debug("service fingerprint: " + encodedServiceFingerprint);
			try {
				this.serviceFingerprint = Hex
						.decodeHex(encodedServiceFingerprint.toCharArray());
			} catch (DecoderException e) {
				throw new ServletException(
						"service fingerprint decoding error: " + e.getMessage(),
						e);
			}
		} else {
			this.serviceFingerprint = null;
		}
	}

	private String getRequiredInitParameter(ServletConfig config,
			String paramName) throws ServletException {
		String paramValue = config.getInitParameter(paramName);
		if (null == paramValue) {
			throw new ServletException("missing init-param: " + paramName);
		}
		return paramValue;
	}

	private void clearAllSessionAttribute(HttpServletRequest request) {
		HttpSession httpSession = request.getSession();
		httpSession.removeAttribute(this.errorMessageSessionAttribute);
		httpSession.removeAttribute(this.signedDocumentSessionAttribute);
		if (null != this.signatureCertificateSessionAttribute) {
			httpSession
					.removeAttribute(this.signatureCertificateSessionAttribute);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		showErrorPage("DSS response processor not available via GET", request,
				response);
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		LOG.debug("doPost");
		clearAllSessionAttribute(request);

		/*
		 * Decode all incoming parameters.
		 */
		String signatureStatus = request
				.getParameter(SIGNATURE_STATUS_PARAMETER);
		if (null == signatureStatus) {
			String msg = SIGNATURE_RESPONSE_PARAMETER
					+ " parameter not present";
			LOG.error(msg);
			showErrorPage(msg, request, response);
			return;
		}
		LOG.debug("signature status: " + signatureStatus);
		HttpSession httpSession = request.getSession();
		if (false == "OK".equals(signatureStatus)) {
			showErrorPage("invalid signature status: " + signatureStatus,
					request, response);
			return;
		}

		String signatureResponse = request
				.getParameter(SIGNATURE_RESPONSE_PARAMETER);
		if (null == signatureResponse) {
			String msg = SIGNATURE_RESPONSE_PARAMETER
					+ " parameter not present";
			LOG.error(msg);
			showErrorPage(msg, request, response);
			return;
		}

		String encodedSignatureCertificate = (String) request
				.getParameter(SIGNATURE_CERTIFICATE_PARAMETER);
		if (null == encodedSignatureCertificate) {
			String msg = SIGNATURE_CERTIFICATE_PARAMETER
					+ " parameter not present";
			LOG.error(msg);
			showErrorPage(msg, request, response);
			return;
		}

		/*
		 * Check service signature.
		 */
		String serviceSigned = (String) request
				.getParameter(SERVICE_SIGNED_PARAMETER);
		if (null != serviceSigned) {
			serviceSigned = URLDecoder.decode(serviceSigned, "UTF-8");
			LOG.debug("service signature present");
			String encodedServiceSignature = (String) request
					.getParameter(SERVICE_SIGNATURE_PARAMETER);
			byte[] serviceSignatureValue = Base64
					.decode(encodedServiceSignature);
			int serviceCertificateChainSize = Integer.parseInt((String) request
					.getParameter(SERVICE_CERTIFICATE_CHAIN_SIZE_PARAMETER));
			List<byte[]> serviceCertificateChain = new LinkedList<byte[]>();
			for (int idx = 1; idx <= serviceCertificateChainSize; idx++) {
				String encodedCertificate = (String) request
						.getParameter(SERVICE_CERTIFICATE_PARAMETER_PREFIX
								+ idx);
				byte[] certificateData = Base64.decode(encodedCertificate);
				serviceCertificateChain.add(certificateData);
			}
			String target = (String) httpSession.getAttribute("target");
			String signatureRequest = (String) httpSession
					.getAttribute("SignatureRequest");
			try {
				verifyServiceSignature(serviceSigned, target, signatureRequest,
						signatureResponse, encodedSignatureCertificate,
						serviceSignatureValue, serviceCertificateChain);
			} catch (Exception e) {
				String msg = "service signature invalid: " + e.getMessage();
				LOG.error(msg, e);
				showErrorPage(msg, request, response);
				return;
			}
		} else {
			if (null != this.serviceFingerprint) {
				/*
				 * In case of a service fingerprint being available, we really
				 * require the eID DSS to send us a service signature.
				 */
				showErrorPage(
						"Service fingerprint available but service signature is missing",
						request, response);
				return;
			}
		}

		/*
		 * Parse all incoming data.
		 */
		byte[] decodedSignatureResponse = Base64.decode(signatureResponse);
		String signedDocument = new String(decodedSignatureResponse);
		LOG.debug("decoded signature response: " + signedDocument);

		byte[] decodedSignatureCertificate = Base64
				.decode(encodedSignatureCertificate);
		X509Certificate signatureCertificate;
		try {
			CertificateFactory certificateFactory = CertificateFactory
					.getInstance("X.509");
			signatureCertificate = (X509Certificate) certificateFactory
					.generateCertificate(new ByteArrayInputStream(
							decodedSignatureCertificate));
		} catch (CertificateException e) {
			String msg = SIGNATURE_CERTIFICATE_PARAMETER
					+ " is not an X509 certificate";
			LOG.error(msg, e);
			showErrorPage(msg, request, response);
			return;
		}

		/*
		 * Push data into the HTTP session.
		 */
		httpSession.setAttribute(this.signedDocumentSessionAttribute,
				signedDocument);
		if (null != this.signatureCertificateSessionAttribute) {
			httpSession.setAttribute(this.signatureCertificateSessionAttribute,
					signatureCertificate);
		}

		/*
		 * Continue work-flow.
		 */
		response.sendRedirect(request.getContextPath() + this.nextPage);
	}

	private void showErrorPage(String errorMessage, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		HttpSession httpSession = request.getSession();
		httpSession.setAttribute(this.errorMessageSessionAttribute,
				errorMessage);
		response.sendRedirect(request.getContextPath() + this.errorPage);
	}

	private void verifyServiceSignature(String serviceSigned, String target,
			String signatureRequest, String signatureResponse,
			String encodedSignatureCertificate, byte[] serviceSignatureValue,
			List<byte[]> serviceCertificateChain) throws CertificateException,
			NoSuchAlgorithmException, InvalidKeyException, SignatureException {
		LOG.debug("verifying service signature");
		byte[] serviceCertificateData = serviceCertificateChain.get(0);
		CertificateFactory certificateFactory = CertificateFactory
				.getInstance("X.509");
		X509Certificate serviceCertificate = (X509Certificate) certificateFactory
				.generateCertificate(new ByteArrayInputStream(
						serviceCertificateData));
		LOG.debug("service identity: "
				+ serviceCertificate.getSubjectX500Principal());
		Signature serviceSignature = Signature.getInstance("SHA1withRSA");
		serviceSignature.initVerify(serviceCertificate);

		StringTokenizer serviceSignedStringTokenizer = new StringTokenizer(
				serviceSigned, ",");
		while (serviceSignedStringTokenizer.hasMoreTokens()) {
			String serviceSignedElement = serviceSignedStringTokenizer
					.nextToken();
			LOG.debug("service signed: " + serviceSignedElement);
			byte[] data;
			if ("target".equals(serviceSignedElement)) {
				data = target.getBytes();
			} else if ("SignatureRequest".equals(serviceSignedElement)) {
				data = signatureRequest.getBytes();
			} else if ("SignatureResponse".equals(serviceSignedElement)) {
				data = signatureResponse.getBytes();
			} else if ("SignatureCertificate".equals(serviceSignedElement)) {
				data = encodedSignatureCertificate.getBytes();
			} else {
				throw new SecurityException("service signed unknown element: "
						+ serviceSignedElement);
			}
			serviceSignature.update(data);
		}

		boolean result = serviceSignature.verify(serviceSignatureValue);
		if (false == result) {
			throw new SecurityException("service signature not valid");
		}
		if (null != this.serviceFingerprint) {
			byte[] actualServiceFingerprint = DigestUtils
					.sha(serviceCertificateData);
			if (false == Arrays.equals(this.serviceFingerprint,
					actualServiceFingerprint)) {
				throw new SecurityException(
						"service certificate fingerprint mismatch");
			}
		}
	}
}
