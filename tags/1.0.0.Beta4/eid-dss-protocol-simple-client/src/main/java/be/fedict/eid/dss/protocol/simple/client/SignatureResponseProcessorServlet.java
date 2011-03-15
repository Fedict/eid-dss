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

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Processes the response from the eID DSS simple protocol.
 * 
 * <p>
 * The following init-params are required:
 * </p>
 * <ul>
 * <li><tt>NextPage</tt>: indicates the page where the flow continues.</li>
 * <li><tt>SignedDocumentSessionAttribute</tt>: indicates which session
 * attribute to use to push in the signed document as byte array as returned by
 * the eID DSS.</li>
 * <li><tt>ErrorPage</tt>: indicates the page to be shown in case of errors.</li>
 * <li><tt>ErrorMessageSessionAttribute</tt>: indicates which session attribute
 * to use for reporting an error. This session attribute can be used on the
 * error page.</li>
 * </ul>
 * 
 * <p>
 * In case the eID DSS puts a service signature on the DSS response, the
 * following init-params become required for validation of the service
 * signature:
 * <ul>
 * <li><tt>TargetSessionAttribute</tt>: refers to the session attribute
 * containing the target page of the DSS signature request.</li>
 * <li><tt>SignatureRequestSessionAttribute</tt>: refers to session attribute
 * containing the base64 encoded signature request.</li>
 * </ul>
 * </p>
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
 * <li><tt>CancelPage</tt>: the page to be shown in case the user cancelled the
 * eID DSS signature ceremony. If not present the user gets redirected towards
 * the error page.</li>
 * </ul>
 * 
 * @author Frank Cornelis
 * 
 */
public class SignatureResponseProcessorServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(SignatureResponseProcessorServlet.class);

	public static final String NEXT_PAGE_INIT_PARAM = "NextPage";

	public static final String ERROR_PAGE_INIT_PARAM = "ErrorPage";

	public static final String CANCEL_PAGE_INIT_PARAM = "CancelPage";

	public static final String ERROR_MESSAGE_SESSION_ATTRIBUTE_INIT_PARAM = "ErrorMessageSessionAttribute";

	public static final String SIGNED_DOCUMENT_SESSION_ATTRIBUTE_INIT_PARAM = "SignedDocumentSessionAttribute";

	public static final String SIGNATURE_CERTIFICATE_SESSION_ATTRIBUTE_INIT_PARAM = "SignatureCertificateSessionAttribute";

	public static final String SERVICE_FINGERPRINT_INIT_PARAM = "ServiceFingerprint";

	public static final String TARGET_SESSION_ATTRIBUTE_INIT_PARAM = "TargetSessionAttribute";

	public static final String SIGNATURE_REQUEST_SESSION_ATTRIBUTE_INIT_PARAM = "SignatureRequestSessionAttribute";

	private String nextPage;

	private String errorPage;

	private String cancelPage;

	private String errorMessageSessionAttribute;

	private String signedDocumentSessionAttribute;

	private String signatureCertificateSessionAttribute;

	private String targetSessionAttribute;

	private String signatureRequestSessionAttribute;

	private SignatureResponseProcessor signatureResponseProcessor;

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
		byte[] serviceFingerprint;
		if (null != encodedServiceFingerprint) {
			LOG.debug("service fingerprint: " + encodedServiceFingerprint);
			try {
				serviceFingerprint = Hex.decodeHex(encodedServiceFingerprint
						.toCharArray());
			} catch (DecoderException e) {
				throw new ServletException(
						"service fingerprint decoding error: " + e.getMessage(),
						e);
			}
		} else {
			serviceFingerprint = null;
		}
		this.signatureResponseProcessor = new SignatureResponseProcessor(
				serviceFingerprint);

		this.targetSessionAttribute = config
				.getInitParameter(TARGET_SESSION_ATTRIBUTE_INIT_PARAM);
		this.signatureRequestSessionAttribute = config
				.getInitParameter(SIGNATURE_REQUEST_SESSION_ATTRIBUTE_INIT_PARAM);
		this.cancelPage = config.getInitParameter(CANCEL_PAGE_INIT_PARAM);
	}

	private String getRequiredInitParameter(ServletConfig config,
			String paramName) throws ServletException {
		String paramValue = config.getInitParameter(paramName);
		if (null == paramValue) {
			throw new ServletException("missing init-param: " + paramName);
		}
		return paramValue;
	}

	/**
	 * Clears the used session attributes. Also returns a reference to the
	 * previously signed document.
	 * 
	 * @param httpSession
	 * @return
	 */
	private byte[] clearAllSessionAttribute(HttpSession httpSession) {
		httpSession.removeAttribute(this.errorMessageSessionAttribute);
		byte[] signedDocument = (byte[]) httpSession
				.getAttribute(this.signedDocumentSessionAttribute);
		httpSession.removeAttribute(this.signedDocumentSessionAttribute);
		if (null != this.signatureCertificateSessionAttribute) {
			httpSession
					.removeAttribute(this.signatureCertificateSessionAttribute);
		}
		return signedDocument;
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
		HttpSession httpSession = request.getSession();
		byte[] previousSignedDocument = clearAllSessionAttribute(httpSession);

		String target = (String) httpSession
				.getAttribute(this.targetSessionAttribute);
		String base64encodedSignatureRequest = (String) httpSession
				.getAttribute(this.signatureRequestSessionAttribute);

		SignatureResponse signatureResponse;
		try {
			signatureResponse = this.signatureResponseProcessor.process(
					request, target, base64encodedSignatureRequest);
		} catch (UserCancelledSignatureResponseProcessorException e) {
			if (null != this.cancelPage) {
				LOG.debug("redirecting to cancel page");
				/*
				 * In case of explicit user cancellation we preserve the signed
				 * document session attribute.
				 */
				httpSession.setAttribute(this.signedDocumentSessionAttribute,
						previousSignedDocument);
				response.sendRedirect(request.getContextPath()
						+ this.cancelPage);
				return;
			}
			showErrorPage(e.getMessage(), request, response);
			return;
		} catch (SignatureResponseProcessorException e) {
			showErrorPage(e.getMessage(), request, response);
			return;
		}

		/*
		 * Push data into the HTTP session.
		 */
		httpSession.setAttribute(this.signedDocumentSessionAttribute,
				signatureResponse.getDecodedSignatureResponse());
		if (null != this.signatureCertificateSessionAttribute) {
			httpSession.setAttribute(this.signatureCertificateSessionAttribute,
					signatureResponse.getSignatureCertificate());
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
}
