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
import java.io.UnsupportedEncodingException;
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

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;

/**
 * Processor for signature response messages.
 * 
 * @author Frank Cornelis
 */
public class SignatureResponseProcessor {

	private static final Log LOG = LogFactory
			.getLog(SignatureResponseProcessor.class);

	public static final String SIGNATURE_RESPONSE_PARAMETER = "SignatureResponse";

	public static final String SIGNATURE_RESPONSE_ID_PARAMETER = "SignatureResponseId";

	public static final String SIGNATURE_STATUS_PARAMETER = "SignatureStatus";

	public static final String SIGNATURE_CERTIFICATE_PARAMETER = "SignatureCertificate";

	public static final String SERVICE_SIGNED_PARAMETER = "ServiceSigned";
	public static final String SERVICE_SIGNATURE_PARAMETER = "ServiceSignature";
	public static final String SERVICE_CERTIFICATE_CHAIN_SIZE_PARAMETER = "ServiceCertificateChainSize";
	public static final String SERVICE_CERTIFICATE_PARAMETER_PREFIX = "ServiceCertificate.";

	public static final String RELAY_STATE_PARAMETER = "RelayState";

	private final CertificateFactory certificateFactory;

	private final byte[] serviceFingerprint;

	/**
	 * Main constructor.
	 * 
	 * @param serviceFingerprint
	 *            the service X509 certificate fingerprint (SHA1) used to
	 *            validate the signatory of the service signature.
	 */
	public SignatureResponseProcessor(byte[] serviceFingerprint) {
		try {
			this.certificateFactory = CertificateFactory.getInstance("X.509");
		} catch (CertificateException e) {
			throw new RuntimeException(
					"could not create certificate factory instance: "
							+ e.getMessage(), e);
		}
		this.serviceFingerprint = serviceFingerprint;
	}

	/**
	 * Default constructor. No service certificate fingerprint checking enabled.
	 */
	public SignatureResponseProcessor() {
		this(null);
	}

	/**
	 * Process the incoming DSS response.
	 * 
	 * @param request
	 *            the HTTP servlet request that holds the DSS response.
	 * @param target
	 *            our target URL used for validation of the service signature.
	 * @param base64encodedSignatureRequest
	 *            optional base64 encoded signature request used for validation
	 *            of the service signature. If <code>null</code> meaning
	 *            artifact binding was used, signatureRequestId becomes
	 *            required.
	 * @param signatureRequestId
	 *            optional signature request ID case artifact binding was used.
	 * @param relayState
	 *            optional relayState param
	 * @return the signature response DTO.
	 * @throws SignatureResponseProcessorException
	 *             in case something went wrong.
	 */
	public SignatureResponse process(HttpServletRequest request, String target,
			String base64encodedSignatureRequest, String signatureRequestId,
			String relayState) throws SignatureResponseProcessorException {
		/*
		 * Decode all incoming parameters.
		 */
		String signatureStatus = request
				.getParameter(SIGNATURE_STATUS_PARAMETER);
		if (null == signatureStatus) {
			String msg = SIGNATURE_STATUS_PARAMETER + " parameter not present";
			LOG.error(msg);
			throw new SignatureResponseProcessorException(msg);
		}
		LOG.debug("signature status: " + signatureStatus);
		if (!"OK".equals(signatureStatus)) {
			String msg = "invalid signature status: " + signatureStatus;
			LOG.error(msg);
			if ("USER_CANCELLED".equals(signatureStatus)) {
				throw new UserCancelledSignatureResponseProcessorException(
						"user cancelled");
			}
			throw new SignatureResponseProcessorException(msg);
		}

		String signatureResponse = request
				.getParameter(SIGNATURE_RESPONSE_PARAMETER);
		String signatureResponseId = request
				.getParameter(SIGNATURE_RESPONSE_ID_PARAMETER);

		if (null == signatureResponse && null == signatureResponseId) {
			String msg = "No " + SIGNATURE_RESPONSE_PARAMETER + " or "
					+ SIGNATURE_RESPONSE_ID_PARAMETER + " parameter found!";
			LOG.error(msg);
			throw new SignatureResponseProcessorException(msg);
		}

		String encodedSignatureCertificate = request
				.getParameter(SIGNATURE_CERTIFICATE_PARAMETER);
		if (null == encodedSignatureCertificate) {
			String msg = SIGNATURE_CERTIFICATE_PARAMETER
					+ " parameter not present";
			LOG.error(msg);
			throw new SignatureResponseProcessorException(msg);
		}

		/*
		 * Validate RelayState if needed.
		 */
		String responseRelayState = request.getParameter(RELAY_STATE_PARAMETER);
		if (null != relayState) {
			if (!relayState.equals(responseRelayState)) {
				String msg = "Returned relayState \"" + responseRelayState
						+ "\" " + "does not match expected RelayState: \""
						+ relayState + "\"";
				LOG.error(msg);
				throw new SignatureResponseProcessorException(msg);
			}
		}

		/*
		 * Check service signature.
		 */
		String encodedServiceSigned = request
				.getParameter(SERVICE_SIGNED_PARAMETER);
		if (null != encodedServiceSigned) {
			LOG.debug("service signature present");
			String serviceSigned;
			try {
				serviceSigned = URLDecoder
						.decode(encodedServiceSigned, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new SignatureResponseProcessorException(
						"URL decoder error: " + e.getMessage());
			}
			String encodedServiceSignature = request
					.getParameter(SERVICE_SIGNATURE_PARAMETER);
			if (null == encodedServiceSignature) {
				throw new SignatureResponseProcessorException("missing "
						+ SERVICE_SIGNATURE_PARAMETER);
			}
			byte[] serviceSignatureValue = Base64
					.decode(encodedServiceSignature);

			/*
			 * Parse the service certificate chain.
			 */
			int serviceCertificateChainSize = Integer.parseInt(request
					.getParameter(SERVICE_CERTIFICATE_CHAIN_SIZE_PARAMETER));
			List<X509Certificate> serviceCertificateChain = new LinkedList<X509Certificate>();
			for (int idx = 1; idx <= serviceCertificateChainSize; idx++) {
				String encodedCertificate = request
						.getParameter(SERVICE_CERTIFICATE_PARAMETER_PREFIX
								+ idx);
				byte[] certificateData = Base64.decode(encodedCertificate);
				X509Certificate certificate;
				try {
					certificate = (X509Certificate) this.certificateFactory
							.generateCertificate(new ByteArrayInputStream(
									certificateData));
				} catch (CertificateException e) {
					throw new SignatureResponseProcessorException(
							"cert decoding error: " + e.getMessage());
				}
				serviceCertificateChain.add(certificate);
			}

			if (null == target) {
				throw new SignatureResponseProcessorException(
						"target parameter required for validation of service signature");
			}

			if (null == base64encodedSignatureRequest
					&& null == signatureRequestId) {
				throw new SignatureResponseProcessorException(
						"base64encodedSignatureRequest or signatureRequestId "
								+ "required for validation of service signature");
			}
			try {
				verifyServiceSignature(serviceSigned, target,
						base64encodedSignatureRequest, signatureRequestId,
						signatureResponse, signatureResponseId,
						encodedSignatureCertificate, serviceSignatureValue,
						serviceCertificateChain);
			} catch (Exception e) {
				String msg = "service signature invalid: " + e.getMessage();
				LOG.error(msg, e);
				throw new SignatureResponseProcessorException(msg);
			}
		} else {
			if (null != this.serviceFingerprint) {
				/*
				 * In case of a service fingerprint being available, we really
				 * require the eID DSS to send us a service signature.
				 */
				throw new SignatureResponseProcessorException(
						"Service fingerprint available but service signature is missing");
			}
		}

		/*
		 * Parse all incoming data.
		 */
		byte[] decodedSignatureResponse = null;
		if (null != signatureResponse) {
			decodedSignatureResponse = Base64.decode(signatureResponse);
			LOG.debug("decoded signature response size: "
					+ decodedSignatureResponse.length);
		}

		byte[] decodedSignatureCertificate = Base64
				.decode(encodedSignatureCertificate);
		X509Certificate signatureCertificate;
		try {
			signatureCertificate = (X509Certificate) this.certificateFactory
					.generateCertificate(new ByteArrayInputStream(
							decodedSignatureCertificate));
		} catch (CertificateException e) {
			String msg = SIGNATURE_CERTIFICATE_PARAMETER
					+ " is not an X509 certificate";
			LOG.error(msg, e);
			throw new SignatureResponseProcessorException(msg);
		}

		/*
		 * Construct result DTO.
		 */
		return new SignatureResponse(decodedSignatureResponse,
				signatureResponseId, signatureCertificate);
	}

	private void verifyServiceSignature(String serviceSigned, String target,
			String signatureRequest, String signatureRequestId,
			String signatureResponse, String signatureResponseId,
			String encodedSignatureCertificate, byte[] serviceSignatureValue,
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
			} else if ("SignatureResponse".equals(serviceSignedElement)) {
				data = signatureResponse.getBytes();
			} else if ("SignatureResponseId".equals(serviceSignedElement)) {
				data = signatureResponseId.getBytes();
			} else if ("SignatureCertificate".equals(serviceSignedElement)) {
				data = encodedSignatureCertificate.getBytes();
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

		if (null != this.serviceFingerprint) {
			LOG.debug("checking service fingerprint");
			byte[] actualServiceFingerprint = DigestUtils
					.sha(serviceCertificate.getEncoded());
			if (!Arrays.equals(this.serviceFingerprint,
					actualServiceFingerprint)) {
				throw new SecurityException(
						"service certificate fingerprint mismatch");
			}
		}
	}
}
