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
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.log.JdkLogChute;

/**
 * Utility class for sending out DSS Simple Protocol Signature Requests.
 * 
 * @author Wim Vandenhaute
 */
public abstract class SignatureRequestUtil {

	private static final Log LOG = LogFactory
			.getLog(SignatureRequestUtil.class);

	private static final String POST_BINDING_TEMPLATE = "be/fedict/eid/dss/protocol/simple/client/dss-post-binding.vm";

	/**
	 * Creates a Signature Request and posts it to the eID DSS Service. If a SP
	 * identity is specified, a service signature will be added.
	 * 
	 * @param signatureRequest
	 *            optional signature request containing the base64 encoded
	 *            document to be siged. If <code>null</code> signatureRequestId
	 *            becomes required.
	 * @param signatureRequestId
	 *            optional signature request ID, which is the ID returned by the
	 *            eID DSS Web Service after a "store" operation. If
	 *            <code>null</code> signatureRequest becomes required.
	 * @param contentType
	 *            optional content type of the document to be signed
	 * @param dssDestination
	 *            eID DSS Protocol Entry point which will handle the signature
	 *            request.
	 * @param spDestination
	 *            SP destination that will handle the returned DSS Signature
	 *            Response.
	 * @param relayState
	 *            optional relayState to be included (if not <code>null</code>)
	 *            in the signature request.
	 * @param spIdentity
	 *            optional SP Identity, if present the signature request will be
	 *            signed.
	 * @param response
	 *            HTTP Servlet Response used for posting the signature request.
	 * @param language
	 *            optional language indication which the eID DSS will use.
	 * @throws SignatureException
	 *             exception setting signature.
	 * @throws InvalidKeyException
	 *             SP Identity key is invalid
	 * @throws NoSuchAlgorithmException
	 *             Signature algorithm not available
	 * @throws CertificateEncodingException
	 *             failed to encode the certificate chain of the SP Identity
	 * @throws IOException
	 *             IO Exception
	 */
	public static void sendRequest(String signatureRequest,
			String signatureRequestId, String contentType,
			String dssDestination, String spDestination, String relayState,
			KeyStore.PrivateKeyEntry spIdentity, HttpServletResponse response,
			String language)

	throws SignatureException, InvalidKeyException, NoSuchAlgorithmException,
			CertificateEncodingException, IOException {

		// construct service signature
		ServiceSignatureDO serviceSignature = null;
		if (null != spIdentity) {
			serviceSignature = getServiceSignature(spIdentity,
					signatureRequest, signatureRequestId, spDestination,
					language, contentType, relayState);
		}

		Properties velocityProperties = new Properties();
		velocityProperties.setProperty("resource.loader", "class");
		velocityProperties.setProperty(
				RuntimeConstants.RUNTIME_LOG_LOGSYSTEM_CLASS,
				JdkLogChute.class.getName());
		velocityProperties.setProperty(JdkLogChute.RUNTIME_LOG_JDK_LOGGER,
				SignatureRequestUtil.class.getName());
		velocityProperties
				.setProperty("class.resource.loader.class",
						"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		velocityProperties.setProperty("file.resource.loader.cache ", "false");
		VelocityEngine velocityEngine;
		try {
			velocityEngine = new VelocityEngine(velocityProperties);
			velocityEngine.init();
		} catch (Exception e) {
			throw new RuntimeException("could not initialize velocity engine",
					e);
		}
		VelocityContext velocityContext = new VelocityContext();
		velocityContext.put("action", dssDestination);
		if (null != signatureRequest) {
			velocityContext.put("SignatureRequest", signatureRequest);
		}
		if (null != signatureRequestId) {
			velocityContext.put("SignatureRequestId", signatureRequestId);
		}
		if (null != contentType) {
			velocityContext.put("ContentType", contentType);
		}
		if (null != relayState) {
			velocityContext.put("RelayState", relayState);
		}
		if (null != dssDestination) {
			velocityContext.put("Target", spDestination);
		}
		if (null != language) {
			velocityContext.put("Language", language);
		}
		if (null != serviceSignature) {
			velocityContext.put("ServiceSigned",
					serviceSignature.getServiceSigned());
			velocityContext.put("ServiceSignature",
					serviceSignature.getServiceSignature());
			velocityContext.put("ServiceCertificateChainSize",
					serviceSignature.getServiceCertificateChainSize());
			velocityContext.put("ServiceCertificates",
					serviceSignature.getServiceCertificates());
		}

		Template template;
		try {
			template = velocityEngine.getTemplate(POST_BINDING_TEMPLATE);
		} catch (Exception e) {
			throw new RuntimeException("Velocity template error: "
					+ e.getMessage(), e);
		}

		response.setContentType("text/html; charset=UTF-8");
		template.merge(velocityContext, response.getWriter());
	}

	/**
	 * Constructs a DSS Simple Protocol service signature.
	 * <p/>
	 * If no spIdentity is specified returns <code>null</code>
	 * 
	 * @param spIdentity
	 *            the SP Identity used for signing.
	 * @param signatureRequest
	 *            signature request, if <code>null</code> signatureRequestId
	 *            needs to be specified.
	 * @param signatureRequestId
	 *            signature request ID, if <code>null</code>, signatureRequest
	 *            needs to be specified
	 * @param target
	 *            required target
	 * @param language
	 *            optional language param
	 * @param contentType
	 *            optional document content type
	 * @param relayState
	 *            optional relay state
	 * @return service signature DO containing the signature value, service
	 *         signed property listing up all signed properties and the SP
	 *         certificate chain.
	 * @throws NoSuchAlgorithmException
	 *             algorithm to sign/digest not found.
	 * @throws InvalidKeyException
	 *             signing key not valid
	 * @throws SignatureException
	 *             signature creation failure
	 * @throws CertificateEncodingException
	 *             certificate encoding failure
	 */
	public static ServiceSignatureDO getServiceSignature(

	KeyStore.PrivateKeyEntry spIdentity, String signatureRequest,
			String signatureRequestId, String target, String language,
			String contentType, String relayState)

	throws NoSuchAlgorithmException, InvalidKeyException, SignatureException,
			CertificateEncodingException {

		LOG.debug("get service signature");

		if (null == spIdentity) {
			LOG.warn("No SP Identity specified, no signature added.");
			return null;
		}
		if (null == signatureRequest && null == signatureRequestId) {
			throw new RuntimeException("Either \"SignatureRequest\" or "
					+ "\"SignatureRequestId\" needs to be provided.");
		}

		// construct service signature
		// TODO: configurable?
		Signature serviceSignature = Signature.getInstance("SHA1withRSA");
		serviceSignature.initSign(spIdentity.getPrivateKey());

		serviceSignature.update(target.getBytes());
		if (null != signatureRequest) {
			serviceSignature.update(signatureRequest.getBytes());
		} else {
			serviceSignature.update(signatureRequestId.getBytes());
		}
		if (null != language) {
			serviceSignature.update(language.getBytes());
		}
		if (null != contentType) {
			serviceSignature.update(contentType.getBytes());
		}
		if (null != relayState) {
			serviceSignature.update(relayState.getBytes());
		}

		byte[] serviceSignatureValue = serviceSignature.sign();

		String encodedServiceSignature = Base64
				.encodeBase64String(serviceSignatureValue);

		// construct service signed
		String serviceSigned = "target";
		if (null != signatureRequest) {
			serviceSigned += ",SignatureRequest";
		} else {
			serviceSigned += ",SignatureRequestId";
		}
		if (null != language) {
			serviceSigned += ",language";
		}
		if (null != contentType) {
			serviceSigned += ",ContentType";
		}
		if (null != relayState) {
			serviceSigned += ",RelayState";
		}

		// construct service certificate chain
		java.security.cert.Certificate[] serviceCertificateChain = spIdentity
				.getCertificateChain();
		String serviceCertificateChainSize = Integer
				.toString(serviceCertificateChain.length);

		List<String> serviceCertificates = new LinkedList<String>();
		for (java.security.cert.Certificate certificate : serviceCertificateChain) {
			String encodedServiceCertificate = Base64
					.encodeBase64String(certificate.getEncoded());
			serviceCertificates.add(encodedServiceCertificate);
		}

		return new ServiceSignatureDO(serviceSigned, encodedServiceSignature,
				serviceCertificateChainSize, serviceCertificates);
	}

}
