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

import be.fedict.eid.dss.client.DigitalSignatureServiceClient;
import be.fedict.eid.dss.client.DocumentNotFoundException;

/**
 * Processes the response from the eID DSS simple protocol.
 * <p/>
 * <p>
 * The following init-params are required:
 * </p>
 * <ul>
 * <li><tt>NextPage</tt>: indicates the page where the flow continues.</li>
 * <li><tt>ErrorPage</tt>: indicates the page to be shown in case of errors.</li>
 * <li><tt>ErrorMessageSessionAttribute</tt>: indicates which session attribute
 * to use for reporting an error. This session attribute can be used on the
 * error page.</li>
 * </ul>
 * <p/>
 * <p>
 * In case the eID DSS puts a service signature on the DSS response, the
 * following init-params become required for validation of the service
 * signature:
 * <ul>
 * <li><tt>TargetSessionAttribute</tt>: refers to the session attribute
 * containing the target page of the DSS signature request.</li>
 * <li><tt>SignatureRequestSessionAttribute</tt> or
 * <tt>SignatureRequestIdSessionAttribute</tt>: refers to session attribute
 * containing the base64 encoded signature request respectively the signature
 * request ID case artifact binding is used.</li>
 * </ul>
 * </p>
 * <p/>
 * <p>
 * The following init-params are optional:
 * </p>
 * <ul>
 * <li><tt>SignedDocumentSessionAttribute</tt>: indicates which session
 * attribute to use to push in the signed document as byte array as returned by
 * the eID DSS. If not specified, it assumer a response for the DSS Artifact
 * Binding where the application wishes to manually call the eID DSS Web Service
 * for retrieval of the signed document. This implies the
 * <tt>SignatureResponseIdSessionAttribute</tt> param to be required, which will
 * hold the id of the signed document, temporarily stored at the eID DSS web
 * service.</li>
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
 */
public class SignatureResponseProcessorServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(SignatureResponseProcessorServlet.class);

	// Flow
	public static final String IFRAME_INIT_PARAM = "IFrame";
	public static final String NEXT_PAGE_INIT_PARAM = "NextPage";
	public static final String ERROR_PAGE_INIT_PARAM = "ErrorPage";
	public static final String CANCEL_PAGE_INIT_PARAM = "CancelPage";
	public static final String ERROR_MESSAGE_SESSION_ATTRIBUTE_INIT_PARAM = "ErrorMessageSessionAttribute";

	// Response
	public static final String SIGNED_DOCUMENT_SESSION_ATTRIBUTE_INIT_PARAM = "SignedDocumentSessionAttribute";
	public static final String SIGNATURE_RESPONSE_ID_SESSION_ATTRIBUTE_INIT_PARAM = "SignatureResponseIdSessionAttribute";
	public static final String SIGNATURE_CERTIFICATE_SESSION_ATTRIBUTE_INIT_PARAM = "SignatureCertificateSessionAttribute";
	public static final String SERVICE_FINGERPRINT_INIT_PARAM = "ServiceFingerprint";

	// WS Client
	public static final String DSS_WS_URL_INIT_PARAM = "DssWSUrl";
	public static final String DSS_WS_PROXY_HOST_INIT_PARAM = "DssWSProxyHost";
	public static final String DSS_WS_PROXY_PORT_INIT_PARAM = "DssWSProxyPort";

	// Request
	public static final String TARGET_SESSION_ATTRIBUTE_INIT_PARAM = "TargetSessionAttribute";
	public static final String SIGNATURE_REQUEST_SESSION_ATTRIBUTE_INIT_PARAM = "SignatureRequestSessionAttribute";
	public static final String SIGNATURE_REQUEST_ID_SESSION_ATTRIBUTE_INIT_PARAM = "SignatureRequestIdSessionAttribute";
	public static final String RELAY_STATE_SESSION_ATTRIBUTE_INIT_PARAM = "RelayStateSessionAttribute";

	// Runtime
	public static final String SIGNATURE_RESPONSE_SERVICE_INIT_PARAM = "SignatureResponseService";

	// Flow config
	private boolean iframe;
	private String nextPage;
	private String errorPage;
	private String cancelPage;
	private String errorMessageSessionAttribute;

	// Response config
	private String signedDocumentSessionAttribute;
	private String signatureResponseIdSessionAttribute;
	private String signatureCertificateSessionAttribute;

	// WS CLient config
	private String dssWSUrl;
	private String dssWSProxyHost;
	private int dssWSProxyPort;

	// Request config
	private String targetSessionAttribute;
	private String signatureRequestSessionAttribute;
	private String signatureRequestIdSessionAttribute;
	private String relayStateSessionAttribute;

	// Runtime config
	private ServiceLocator<SignatureResponseService> signatureResponseServiceLocator;

	private SignatureResponseProcessor signatureResponseProcessor;

	@Override
	public void init(ServletConfig config) throws ServletException {

		// flow
		LOG.debug("init");
		this.iframe = null != config.getInitParameter(IFRAME_INIT_PARAM);
		this.nextPage = getRequiredInitParameter(config, NEXT_PAGE_INIT_PARAM);
		LOG.debug("next page: " + this.nextPage);
		this.cancelPage = config.getInitParameter(CANCEL_PAGE_INIT_PARAM);
		this.errorPage = getRequiredInitParameter(config, ERROR_PAGE_INIT_PARAM);
		LOG.debug("error page: " + this.errorPage);
		this.errorMessageSessionAttribute = getRequiredInitParameter(config,
				ERROR_MESSAGE_SESSION_ATTRIBUTE_INIT_PARAM);
		LOG.debug("error message session attribute: "
				+ this.errorMessageSessionAttribute);

		// WS client config
		this.dssWSUrl = config.getInitParameter(DSS_WS_URL_INIT_PARAM);
		this.dssWSProxyHost = config
				.getInitParameter(DSS_WS_PROXY_HOST_INIT_PARAM);
		String dssWSProxyPortString = config
				.getInitParameter(DSS_WS_PROXY_PORT_INIT_PARAM);
		if (null != dssWSProxyPortString) {
			this.dssWSProxyPort = Integer.parseInt(dssWSProxyPortString);
		}
		if (null != this.dssWSUrl) {
			LOG.debug("DSS WS: " + this.dssWSUrl + " (proxy="
					+ this.dssWSProxyHost + ":" + this.dssWSProxyPort + ")");
		}

		// Response Config
		this.signedDocumentSessionAttribute = config
				.getInitParameter(SIGNED_DOCUMENT_SESSION_ATTRIBUTE_INIT_PARAM);
		this.signatureResponseIdSessionAttribute = config
				.getInitParameter(SIGNATURE_RESPONSE_ID_SESSION_ATTRIBUTE_INIT_PARAM);
		this.signatureCertificateSessionAttribute = config
				.getInitParameter(SIGNATURE_CERTIFICATE_SESSION_ATTRIBUTE_INIT_PARAM);

		if (null == this.signedDocumentSessionAttribute
				&& null == this.signatureResponseIdSessionAttribute) {
			throw new ServletException("Need \""
					+ SIGNED_DOCUMENT_SESSION_ATTRIBUTE_INIT_PARAM + "\" or \""
					+ SIGNATURE_RESPONSE_ID_SESSION_ATTRIBUTE_INIT_PARAM
					+ "\" init params");
		}

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

		// Request Config
		this.targetSessionAttribute = config
				.getInitParameter(TARGET_SESSION_ATTRIBUTE_INIT_PARAM);
		this.signatureRequestSessionAttribute = config
				.getInitParameter(SIGNATURE_REQUEST_SESSION_ATTRIBUTE_INIT_PARAM);
		this.signatureRequestIdSessionAttribute = config
				.getInitParameter(SIGNATURE_REQUEST_ID_SESSION_ATTRIBUTE_INIT_PARAM);
		this.relayStateSessionAttribute = config
				.getInitParameter(RELAY_STATE_SESSION_ATTRIBUTE_INIT_PARAM);

		// runtime config
		this.signatureResponseServiceLocator = new ServiceLocator<SignatureResponseService>(
				SIGNATURE_RESPONSE_SERVICE_INIT_PARAM, config);

		// Construct response processor
		this.signatureResponseProcessor = new SignatureResponseProcessor(
				serviceFingerprint);
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
	 *            the http session
	 * @return reference to previously signed document
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
		String signatureRequestId = (String) httpSession
				.getAttribute(this.signatureRequestIdSessionAttribute);
		String relayState = (String) httpSession
				.getAttribute(this.relayStateSessionAttribute);
		LOG.debug("RelayState: " + relayState);

		SignatureResponse signatureResponse;
		try {
			signatureResponse = this.signatureResponseProcessor.process(
					request, target, base64encodedSignatureRequest,
					signatureRequestId, relayState);
		} catch (UserCancelledSignatureResponseProcessorException e) {
			if (null != this.cancelPage) {
				LOG.debug("redirecting to cancel page");
				/*
				 * In case of explicit user cancellation we preserve the signed
				 * document session attribute.
				 */
				httpSession.setAttribute(this.signedDocumentSessionAttribute,
						previousSignedDocument);
				redirectTo(response, request.getContextPath() + this.cancelPage);
				return;
			}
			showErrorPage(e.getMessage(), request, response);
			return;
		} catch (SignatureResponseProcessorException e) {
			showErrorPage(e.getMessage(), request, response);
			return;
		}

		byte[] decodedSignatureResponse = signatureResponse
				.getDecodedSignatureResponse();
		String signatureResponseId = signatureResponse.getSignatureResponseId();

		/*
		 * Check signed document passed along, if not signatureResponseId should
		 * be present.
		 * 
		 * If configuration is available for DSS WS Client fetch the signed
		 * document here, else SP has to do it himself via signatureResponseId
		 * pushed on session.
		 */
		if (null == decodedSignatureResponse) {

			if (null == signatureResponseId) {
				showErrorPage("No signed document nor response ID found!",
						request, response);
				return;
			}

			// check WS client config available
			String dssWSUrl;
			if (this.signatureResponseServiceLocator.isConfigured()) {
				SignatureResponseService signatureResponseService = this.signatureResponseServiceLocator
						.locateService();
				dssWSUrl = signatureResponseService.getDssWSUrl();
				LOG.debug("DSS WS URL: " + dssWSUrl);
			} else {
				dssWSUrl = this.dssWSUrl;
			}
			if (null != dssWSUrl) {
				DigitalSignatureServiceClient dssClient = new DigitalSignatureServiceClient(
						dssWSUrl);
				if (null != this.dssWSProxyHost) {
					dssClient
							.setProxy(this.dssWSProxyHost, this.dssWSProxyPort);
				} else {
					// disable previously set proxy
					dssClient.setProxy(null, 0);
				}
				try {
					decodedSignatureResponse = dssClient
							.retrieve(signatureResponseId);
				} catch (DocumentNotFoundException e) {
					showErrorPage("Document not found at the eID DSS WS.",
							request, response);
					return;
				}

			} else {

				if (null == this.signatureResponseIdSessionAttribute) {
					showErrorPage("No SignatureResponseId session attribute "
							+ "specified, aborting...", request, response);
					return;
				}
				// push response ID on session
				httpSession.setAttribute(
						this.signatureResponseIdSessionAttribute,
						signatureResponseId);

			}

			if (null != this.signatureRequestSessionAttribute) {
				httpSession
						.removeAttribute(this.signatureRequestSessionAttribute);
			}
			if (null != this.signatureRequestIdSessionAttribute) {
				httpSession
						.removeAttribute(this.signatureRequestIdSessionAttribute);
			}
		}

		/*
		 * Push data into the HTTP session.
		 */
		httpSession.setAttribute(this.signedDocumentSessionAttribute,
				decodedSignatureResponse);
		if (null != this.signatureCertificateSessionAttribute) {
			httpSession.setAttribute(this.signatureCertificateSessionAttribute,
					signatureResponse.getSignatureCertificate());
		}

		/*
		 * Continue work-flow.
		 */
		redirectTo(response, request.getContextPath() + this.nextPage);
	}

	private void showErrorPage(String errorMessage, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		HttpSession httpSession = request.getSession();
		httpSession.setAttribute(this.errorMessageSessionAttribute,
				errorMessage);
		redirectTo(response, request.getContextPath() + this.errorPage);
	}

	private void redirectTo(HttpServletResponse response, String location)
			throws IOException {

		if (iframe) {
			response.setContentType("text/html");
			response.getWriter().println("<html>");
			response.getWriter().println("<head>");
			response.getWriter().println("<script type=\"text/javascript\">");
			response.getWriter().println(
					"top.location.replace(\"" + location + "\");");
			response.getWriter().println("</script>");
			response.getWriter().println("</head>");
			response.getWriter().println("</html>");
		} else {
			response.sendRedirect(location);

		}
	}
}
