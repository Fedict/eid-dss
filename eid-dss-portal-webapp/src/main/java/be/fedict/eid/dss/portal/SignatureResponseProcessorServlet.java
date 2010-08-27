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

package be.fedict.eid.dss.portal;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Processes the response from the eID DSS.
 * 
 * TODO: factor out this code to have some SDK components.
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

	public static final String SIGNATURE_STATUS_SESSION_ATTRIBUTE = "SignatureStatus";

	public static final String SIGNED_DOCUMENT_SESSION_ATTRIBUTE = SignatureResponseProcessorServlet.class
			.getName() + ".signedDocument";

	private String nextPage;

	private String errorPage;

	@Override
	public void init(ServletConfig config) throws ServletException {
		LOG.debug("init");
		this.nextPage = config.getInitParameter(NEXT_PAGE_INIT_PARAM);
		if (null == this.nextPage) {
			throw new ServletException("missing init-param: "
					+ NEXT_PAGE_INIT_PARAM);
		}
		LOG.debug("next page: " + this.nextPage);
		this.errorPage = config.getInitParameter(ERROR_PAGE_INIT_PARAM);
		if (null == this.errorPage) {
			throw new ServletException("missing init-param: "
					+ ERROR_PAGE_INIT_PARAM);
		}
		LOG.debug("error page: " + this.errorPage);
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		LOG.debug("doPost");
		/*
		 * Decode all incoming parameters.
		 */
		String signatureStatus = request
				.getParameter(SIGNATURE_STATUS_PARAMETER);
		if (null == signatureStatus) {
			String msg = SIGNATURE_RESPONSE_PARAMETER
					+ " parameter not present";
			LOG.error(msg);
			showErrorPage(msg, response);
			return;
		}
		LOG.debug("signature status: " + signatureStatus);
		HttpSession httpSession = request.getSession();
		if (false == "OK".equals(signatureStatus)) {
			// signature status is used by the error page.
			httpSession.setAttribute(SIGNATURE_STATUS_SESSION_ATTRIBUTE,
					signatureStatus);
			response.sendRedirect(this.errorPage);
			return;
		}

		String signatureResponse = request
				.getParameter(SIGNATURE_RESPONSE_PARAMETER);
		if (null == signatureResponse) {
			String msg = SIGNATURE_RESPONSE_PARAMETER
					+ " parameter not present";
			LOG.error(msg);
			showErrorPage(msg, response);
			return;
		}

		String encodedSignatureCertificate = (String) request
				.getParameter(SIGNATURE_CERTIFICATE_PARAMETER);
		if (null == encodedSignatureCertificate) {
			String msg = SIGNATURE_CERTIFICATE_PARAMETER
					+ " parameter not present";
			LOG.error(msg);
			showErrorPage(msg, response);
			return;
		}

		/*
		 * Check service signature.
		 */
		String serviceSigned = (String) request
				.getParameter(SERVICE_SIGNED_PARAMETER);
		if (null != serviceSigned) {
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
				showErrorPage(msg, response);
				return;
			}
		}

		/*
		 * Parse all incoming data.
		 */
		byte[] decodedSignatureResponse = Base64.decode(signatureResponse);
		LOG.debug("decoded signature response: "
				+ new String(decodedSignatureResponse));
		try {
			loadDocument(new ByteArrayInputStream(decodedSignatureResponse));
		} catch (Exception e) {
			String msg = SIGNATURE_RESPONSE_PARAMETER
					+ " is not an XML document";
			LOG.error(msg, e);
			showErrorPage(msg, response);
			return;
		}

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
			showErrorPage(msg, response);
			return;
		}

		/*
		 * Push data into the HTTP session.
		 */
		setSignedDocument(new String(decodedSignatureResponse), httpSession);
		httpSession.setAttribute("signatureCertificate", signatureCertificate);

		/*
		 * Continue work-flow.
		 */
		response.sendRedirect(this.nextPage);
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
	}

	private void setSignedDocument(String signedDocument,
			HttpSession httpSession) {
		httpSession.setAttribute(SIGNED_DOCUMENT_SESSION_ATTRIBUTE,
				signedDocument);
	}

	public static String getSignedDocument(HttpSession httpSession) {
		String signedDocument = (String) httpSession
				.getAttribute(SIGNED_DOCUMENT_SESSION_ATTRIBUTE);
		return signedDocument;
	}

	private Document loadDocument(InputStream documentInputStream)
			throws ParserConfigurationException, SAXException, IOException {
		InputSource inputSource = new InputSource(documentInputStream);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.parse(inputSource);
		return document;
	}

	private void showErrorPage(String message, HttpServletResponse response)
			throws IOException {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("<head><title>eID DSS Signature Response Processor</title></head>");
		out.println("<body>");
		out.println("<h1>eID DSS Signature Response Processor</h1>");
		out.println("<p>ERROR: " + message + "</p>");
		out.println("</body></html>");
		out.close();
	}
}
