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

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.spi.BrowserPOSTResponse;
import be.fedict.eid.dss.spi.DSSProtocolContext;
import be.fedict.eid.dss.spi.DSSProtocolService;
import be.fedict.eid.dss.spi.DSSRequest;
import be.fedict.eid.dss.spi.SignatureStatus;

/**
 * Implementation of a very simple DSS protocol.
 * 
 * @author Frank Cornelis
 */
public class SimpleDSSProtocolService implements DSSProtocolService {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(SimpleDSSProtocolService.class);

	public static final String TARGET_PARAMETER = "target";
	public static final String SIGNATURE_REQUEST_PARAMETER = "SignatureRequest";
	public static final String SIGNATURE_REQUEST_ID_PARAMETER = "SignatureRequestId";
	public static final String LANGUAGE_PARAMETER = "language";
	public static final String CONTENT_TYPE_PARAMETER = "ContentType";
	public static final String RELAY_STATE_PARAMETER = "RelayState";

	// service signature
	public static final String SERVICE_SIGNED_PARAMETER = "ServiceSigned";
	public static final String SERVICE_SIGNATURE_PARAMETER = "ServiceSignature";
	public static final String SERVICE_CERTIFICATE_CHAIN_SIZE_PARAMETER = "ServiceCertificateChainSize";
	public static final String SERVICE_CERTIFICATE_PARAMETER_PREFIX = "ServiceCertificate.";

	public static final String TARGET_SESSION_ATTRIBUTE = SimpleDSSProtocolService.class
			.getName() + ".Target";
	public static final String SIGNATURE_REQUEST_SESSION_ATTRIBUTE = SimpleDSSProtocolService.class
			.getName() + ".SignatureRequest";
	public static final String SIGNATURE_REQUEST_ID_SESSION_ATTRIBUTE = SimpleDSSProtocolService.class
			.getName() + ".SignatureRequestId";

	private DSSProtocolContext dssContext;

	private CertificateFactory certificateFactory;

	public void init(ServletContext servletContext,
			DSSProtocolContext dssContext) {
		LOG.debug("init");
		this.dssContext = dssContext;

		try {
			this.certificateFactory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			throw new RuntimeException(
					"could not create certificate factory instance: "
							+ e.getMessage(), e);
		}
	}

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

		String relayState = request.getParameter(RELAY_STATE_PARAMETER);
		storeRelayState(relayState, httpSession);

		String signatureRequest = request
				.getParameter(SIGNATURE_REQUEST_PARAMETER);
		String signatureRequestId = request
				.getParameter(SIGNATURE_REQUEST_ID_PARAMETER);
		if (null == signatureRequest && null == signatureRequestId) {
			throw new IllegalArgumentException("Need or "
					+ SIGNATURE_REQUEST_PARAMETER + " or "
					+ SIGNATURE_REQUEST_ID_PARAMETER);
		}

		byte[] decodedSignatureRequest = null;
		String contentType;
		if (null != signatureRequest) {
			/*
			 * Needed during response for service signature.
			 */
			storeSignatureRequest(signatureRequest, httpSession);
			decodedSignatureRequest = Base64.decodeBase64(signatureRequest);
			contentType = request.getParameter(CONTENT_TYPE_PARAMETER);
			LOG.debug("content type: " + contentType);
		} else {
			/*
			 * Needed during response for service signature.
			 */
			storeSignatureRequestId(signatureRequestId, httpSession);
			contentType = request.getParameter(CONTENT_TYPE_PARAMETER);
		}

		List<X509Certificate> serviceCertificateChain = null;
		String serviceSigned = request.getParameter(SERVICE_SIGNED_PARAMETER);
		if (null != serviceSigned) {

			// request service signature validation
			LOG.debug("ServiceSigned: " + serviceSigned);

			serviceCertificateChain = new LinkedList<X509Certificate>();
			String encodedServiceSignature = request
					.getParameter(SERVICE_SIGNATURE_PARAMETER);
			byte[] serviceSignatureValue = Base64
					.decodeBase64(encodedServiceSignature);

			/*
			 * Parse the service certificate chain.
			 */
			int serviceCertificateChainSize = Integer.parseInt(request
					.getParameter(SERVICE_CERTIFICATE_CHAIN_SIZE_PARAMETER));
			for (int idx = 1; idx <= serviceCertificateChainSize; idx++) {
				String encodedCertificate = request
						.getParameter(SERVICE_CERTIFICATE_PARAMETER_PREFIX
								+ idx);
				byte[] certificateData = Base64
						.decodeBase64(encodedCertificate);
				X509Certificate certificate;
				try {
					certificate = (X509Certificate) this.certificateFactory
							.generateCertificate(new ByteArrayInputStream(
									certificateData));
				} catch (CertificateException e) {
					throw new IllegalArgumentException("cert decoding error: "
							+ e.getMessage());
				}
				serviceCertificateChain.add(certificate);

				// verify signature
				verifyServiceSignature(serviceSigned, target, signatureRequest,
						signatureRequestId, contentType, language, relayState,
						serviceSignatureValue, serviceCertificateChain);

			}

		}

		if (null == contentType && null != signatureRequest) {
			contentType = "text/xml";
		}

		return new DSSRequest(decodedSignatureRequest, contentType,
				signatureRequestId, language, target, serviceCertificateChain);
	}

	private void verifyServiceSignature(String serviceSigned, String target,
			String signatureRequest, String signatureRequestId,
			String contentType, String language, String relayState,
			byte[] serviceSignatureValue,
			List<X509Certificate> serviceCertificateChain)
			throws CertificateException, NoSuchAlgorithmException,
			InvalidKeyException, SignatureException {

		LOG.debug("verifying service signature");
		X509Certificate serviceCertificate = serviceCertificateChain.get(0);
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
			} else if ("SignatureRequestId".equals(serviceSignedElement)) {
				data = signatureRequestId.getBytes();
			} else if ("ContentType".equals(serviceSignedElement)) {
				data = contentType.getBytes();
			} else if ("language".equals(serviceSignedElement)) {
				data = language.getBytes();
			} else if ("RelayState".equals(serviceSignedElement)) {
				data = relayState.getBytes();
			} else {
				throw new SecurityException("service signed unknown element: "
						+ serviceSignedElement);
			}
			serviceSignature.update(data);
		}

		boolean valid = serviceSignature.verify(serviceSignatureValue);
		if (!valid) {
			throw new SecurityException("service signature not valid");
		}
	}

	public BrowserPOSTResponse handleResponse(SignatureStatus signatureStatus,
			byte[] signedDocument, String artifact,
			X509Certificate signerCertificate, HttpSession httpSession,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {

		LOG.debug("handleResponse");
		String target = retrieveTarget(httpSession);
		BrowserPOSTResponse browserPOSTResponse = new BrowserPOSTResponse(
				target);
		browserPOSTResponse.addAttribute("SignatureStatus",
				signatureStatus.getStatus());

		/*
		 * Add RelayState if available
		 */
		String relayState = retrieveRelayState(httpSession);
		if (null != relayState) {
			browserPOSTResponse.addAttribute("RelayState", relayState);
		}

		if (SignatureStatus.OK == signatureStatus) {

			String signatureRequest = retrieveSignatureRequest(httpSession);
			String signatureRequestId = retrieveSignatureRequestId(httpSession);
			String encodedSignedDocument = Base64
					.encodeBase64String(signedDocument);

			if (null != signatureRequest) {

				browserPOSTResponse.addAttribute("SignatureResponse",
						encodedSignedDocument);
			} else {

				browserPOSTResponse.addAttribute("SignatureResponseId",
						artifact);

			}

			byte[] derSignerCertificate = signerCertificate.getEncoded();
			String encodedSignatureCertificate = Base64
					.encodeBase64String(derSignerCertificate);
			browserPOSTResponse.addAttribute("SignatureCertificate",
					encodedSignatureCertificate);

			KeyStore.PrivateKeyEntry identityPrivateKeyEntry = this.dssContext
					.getIdentity();
			if (null != identityPrivateKeyEntry) {
				LOG.debug("signing the response");

				if (null != signatureRequest) {
					browserPOSTResponse.addAttribute("ServiceSigned",
							URLEncoder.encode("target,SignatureRequest,"
									+ "SignatureResponse,"
									+ "SignatureCertificate", "UTF-8"));
				} else {
					browserPOSTResponse.addAttribute("ServiceSigned",
							URLEncoder.encode("target,SignatureRequestId,"
									+ "SignatureResponseId,"
									+ "SignatureCertificate", "UTF-8"));
				}

				// service signature
				Signature serviceSignature = Signature
						.getInstance("SHA1withRSA");
				serviceSignature.initSign(identityPrivateKeyEntry
						.getPrivateKey());
				serviceSignature.update(target.getBytes());

				if (null != signatureRequest) {
					serviceSignature.update(signatureRequest.getBytes());
					serviceSignature.update(encodedSignedDocument.getBytes());
				} else {
					serviceSignature.update(signatureRequestId.getBytes());
					serviceSignature.update(artifact.getBytes());
				}

				serviceSignature.update(encodedSignatureCertificate.getBytes());

				byte[] serviceSignatureValue = serviceSignature.sign();

				String encodedServiceSignature = Base64
						.encodeBase64String(serviceSignatureValue);
				browserPOSTResponse.addAttribute("ServiceSignature",
						encodedServiceSignature);

				// service certificate chain
				Certificate[] serviceCertificateChain = identityPrivateKeyEntry
						.getCertificateChain();
				browserPOSTResponse.addAttribute("ServiceCertificateChainSize",
						Integer.toString(serviceCertificateChain.length));
				for (int certIdx = 0; certIdx < serviceCertificateChain.length; certIdx++) {
					Certificate certificate = serviceCertificateChain[certIdx];
					String encodedServiceCertificate = Base64
							.encodeBase64String(certificate.getEncoded());
					browserPOSTResponse.addAttribute("ServiceCertificate."
							+ (certIdx + 1), encodedServiceCertificate);
				}
			}
		}
		return browserPOSTResponse;
	}

	private void storeTarget(String target, HttpSession httpSession) {
		httpSession.setAttribute(TARGET_SESSION_ATTRIBUTE, target);
	}

	private String retrieveTarget(HttpSession httpSession) {
		return (String) httpSession.getAttribute(TARGET_SESSION_ATTRIBUTE);
	}

	private void storeSignatureRequest(String signatureRequest,
			HttpSession httpSession) {
		httpSession.setAttribute(SIGNATURE_REQUEST_SESSION_ATTRIBUTE,
				signatureRequest);
	}

	private String retrieveSignatureRequest(HttpSession httpSession) {
		return (String) httpSession
				.getAttribute(SIGNATURE_REQUEST_SESSION_ATTRIBUTE);
	}

	private void storeSignatureRequestId(String signatureRequestId,
			HttpSession httpSession) {
		httpSession.setAttribute(SIGNATURE_REQUEST_ID_SESSION_ATTRIBUTE,
				signatureRequestId);
	}

	private String retrieveSignatureRequestId(HttpSession httpSession) {
		return (String) httpSession
				.getAttribute(SIGNATURE_REQUEST_ID_SESSION_ATTRIBUTE);
	}

	private void storeRelayState(String relayState, HttpSession httpSession) {
		httpSession.setAttribute(RELAY_STATE_PARAMETER, relayState);
	}

	private String retrieveRelayState(HttpSession httpSession) {
		return (String) httpSession.getAttribute(RELAY_STATE_PARAMETER);
	}
}
