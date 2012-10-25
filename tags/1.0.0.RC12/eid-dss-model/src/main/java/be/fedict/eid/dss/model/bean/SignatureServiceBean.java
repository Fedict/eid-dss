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

package be.fedict.eid.dss.model.bean;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.eid.applet.service.signer.DigestAlgo;
import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.time.TSPTimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.applet.service.spi.AddressDTO;
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.applet.service.spi.IdentityDTO;
import be.fedict.eid.applet.service.spi.SignatureService;
import be.fedict.eid.applet.service.spi.SignatureServiceEx;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.Constants;
import be.fedict.eid.dss.model.DocumentRepository;
import be.fedict.eid.dss.model.ServicesManager;
import be.fedict.eid.dss.model.TrustValidationService;
import be.fedict.eid.dss.spi.DSSDocumentService;
import be.fedict.trust.client.XKMS2Client;

/**
 * XML Signature Service bean. Acts as a proxy towards the actual
 * SignatureService implementation provided by some document service.
 * <p/>
 * 
 * @author Frank Cornelis
 */
@Stateless
@Local(SignatureServiceEx.class)
@LocalBinding(jndiBinding = Constants.DSS_JNDI_CONTEXT + "SignatureServiceBean")
public class SignatureServiceBean implements SignatureServiceEx {

	private static final Log LOG = LogFactory
			.getLog(SignatureServiceBean.class);

	@EJB
	private Configuration configuration;

	@EJB
	private ServicesManager servicesManager;

	@EJB
	private TrustValidationService trustValidationService;

	public String getFilesDigestAlgorithm() {
		return null;
	}

	public DigestInfo preSign(List<DigestInfo> digestInfos,
			List<X509Certificate> signingCertificateChain)
			throws NoSuchAlgorithmException {
		throw new UnsupportedOperationException();
	}

	public void postSign(byte[] signatureValue,
			List<X509Certificate> signingCertificateChain)
			throws SecurityException {

		SignatureService signatureService = getSignatureService(null, null);
		signatureService.postSign(signatureValue, signingCertificateChain);
	}

	private SignatureServiceEx getSignatureService(IdentityDTO identity,
			byte[] photo) {

		XKMS2Client xkms2Client = this.trustValidationService.getXkms2Client();

		String tspUrl = this.configuration.getValue(ConfigProperty.TSP_URL,
				String.class);
		String tspPolicyOid = this.configuration.getValue(
				ConfigProperty.TSP_POLICY_OID, String.class);

		String signTrustDomain = this.configuration.getValue(
				ConfigProperty.SIGN_TRUST_DOMAIN, String.class);
		String tsaTrustDomain = this.configuration.getValue(
				ConfigProperty.TSA_TRUST_DOMAIN, String.class);

		Boolean useHttpProxy = this.configuration.getValue(
				ConfigProperty.HTTP_PROXY_ENABLED, Boolean.class);
		String httpProxyHost = this.configuration.getValue(
				ConfigProperty.HTTP_PROXY_HOST, String.class);
		Integer httpProxyPort = this.configuration.getValue(
				ConfigProperty.HTTP_PROXY_PORT, Integer.class);

		DigestAlgo signatureDigestAlgo = this.configuration.getValue(
				ConfigProperty.SIGNATURE_DIGEST_ALGO, DigestAlgo.class);

		LOG.debug("signatureDigestAlgo: " + signatureDigestAlgo);

		RevocationDataService revocationDataService = new TrustServiceRevocationDataService(
				xkms2Client, signTrustDomain);
		SignatureFacet signatureFacet = new SignerCertificateSignatureFacet();
		TimeStampServiceValidator timeStampServiceValidator = new TrustServiceTimeStampServiceValidator(
				xkms2Client, tsaTrustDomain);
		TSPTimeStampService timeStampService = new TSPTimeStampService(tspUrl,
				timeStampServiceValidator);
		if (useHttpProxy) {
			timeStampService.setProxy(httpProxyHost, httpProxyPort);
		}
		if (null != tspPolicyOid && !tspPolicyOid.isEmpty()) {
			timeStampService.setRequestPolicy(tspPolicyOid);
		}

		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		byte[] document = documentRepository.getDocument();
		if (null == document) {
			throw new RuntimeException("no document to be signed");
		}
		ByteArrayInputStream documentInputStream = new ByteArrayInputStream(
				document);
		String role = documentRepository.getRole();

		OutputStream documentOutputStream = new DocumentRepositoryOutputStream();

		DSSDocumentService documentService = this.servicesManager
				.getDocumentService();
		SignatureServiceEx signatureService;
		try {
			signatureService = documentService.getSignatureService(
					documentInputStream, timeStampService,
					timeStampServiceValidator, revocationDataService,
					signatureFacet, documentOutputStream, role, identity,
					photo, signatureDigestAlgo);
		} catch (Exception e) {
			throw new RuntimeException("error retrieving signature service: "
					+ e.getMessage(), e);
		}
		return signatureService;
	}

	public DigestInfo preSign(List<DigestInfo> digestInfos,
			List<X509Certificate> signingCertificateChain,
			IdentityDTO identity, AddressDTO address, byte[] photo)
			throws NoSuchAlgorithmException {

		SignatureServiceEx signatureService = getSignatureService(identity,
				photo);
		return signatureService.preSign(digestInfos, signingCertificateChain,
				identity, address, photo);
	}
}
