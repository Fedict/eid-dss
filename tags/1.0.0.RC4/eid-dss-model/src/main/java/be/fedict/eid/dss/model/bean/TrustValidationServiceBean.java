/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010-2011 FedICT.
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

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertStore;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.SignerId;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;

import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.TrustValidationService;
import be.fedict.trust.TrustValidator;
import be.fedict.trust.client.XKMS2Client;
import be.fedict.trust.client.exception.RevocationDataNotFoundException;
import be.fedict.trust.client.exception.TrustDomainNotFoundException;
import be.fedict.trust.client.exception.ValidationFailedException;

@Stateless
public class TrustValidationServiceBean implements TrustValidationService {

	private static final Log LOG = LogFactory
			.getLog(TrustValidationServiceBean.class);

	@EJB
	private Configuration configuration;

	/**
	 * {@inheritDoc}
	 */
	public void validate(List<X509Certificate> certificateChain,
			Date validationDate, List<OCSPResp> ocspResponses,
			List<X509CRL> crls) throws CertificateEncodingException,
			TrustDomainNotFoundException, RevocationDataNotFoundException,
			ValidationFailedException {

		String verifyTrustDomain = this.configuration.getValue(
				ConfigProperty.VERIFY_TRUST_DOMAIN, String.class);

		LOG.debug("validating certificate chain");
		LOG.debug("number of CRLs: " + crls.size());
		LOG.debug("number of OCSPs: " + ocspResponses.size());
		getXkms2Client().validate(verifyTrustDomain, certificateChain,
				validationDate, ocspResponses, crls);
	}

	/**
	 * {@inheritDoc}
	 */
	public void validate(TimeStampToken timeStampToken)
			throws CertificateEncodingException, ValidationFailedException,
			TrustDomainNotFoundException, RevocationDataNotFoundException {

		String tsaTrustDomain = this.configuration.getValue(
				ConfigProperty.TSA_TRUST_DOMAIN, String.class);

		LOG.debug("validating timestamp token");
		getXkms2Client().validate(tsaTrustDomain, timeStampToken);
	}

	/**
	 * {@inheritDoc}
	 */
	public XKMS2Client getXkms2Client() {

		String xkmsUrl = this.configuration.getValue(ConfigProperty.XKMS_URL,
				String.class);
		XKMS2Client xkms2Client = new XKMS2Client(xkmsUrl);

		Boolean useHttpProxy = this.configuration.getValue(
				ConfigProperty.HTTP_PROXY_ENABLED, Boolean.class);
		if (null != useHttpProxy && useHttpProxy) {
			String httpProxyHost = this.configuration.getValue(
					ConfigProperty.HTTP_PROXY_HOST, String.class);
			int httpProxyPort = this.configuration.getValue(
					ConfigProperty.HTTP_PROXY_PORT, Integer.class);
			xkms2Client.setProxy(httpProxyHost, httpProxyPort);
		} else {
			// disable previously set proxy
			xkms2Client.setProxy(null, 0);
		}

		return xkms2Client;
	}

	public void validate(TimeStampToken timeStampToken,
			List<OCSPResp> ocspResponses, List<X509CRL> crls)
			throws CertificateEncodingException, TrustDomainNotFoundException,
			RevocationDataNotFoundException, ValidationFailedException,
			NoSuchAlgorithmException, NoSuchProviderException, CMSException,
			CertStoreException {
		LOG.debug("performing historical TSA validation...");
		String tsaTrustDomain = this.configuration.getValue(
				ConfigProperty.TSA_TRUST_DOMAIN, String.class);
		LOG.debug("TSA trust domain: " + tsaTrustDomain);

		Date validationDate = timeStampToken.getTimeStampInfo().getGenTime();
		LOG.debug("TSA validation date is TST time: " + validationDate);
		LOG.debug("# TSA ocsp responses: " + ocspResponses.size());
		LOG.debug("# TSA CRLs: " + crls.size());

		/*
		 * Construct the TSA cert chain.
		 */
		List<X509Certificate> certificateChain = new LinkedList<X509Certificate>();
		CertStore certStore = timeStampToken.getCertificatesAndCRLs(
				"Collection", "BC");
		Collection<? extends Certificate> certificates = certStore
				.getCertificates(null);
		for (Certificate certificate : certificates) {
			certificateChain.add((X509Certificate) certificate);
		}
		if (TrustValidator.isSelfSigned(certificateChain.get(0))) {
			Collections.reverse(certificateChain);
		}

		/*
		 * Check TSA signer.
		 */
		SignerId signerId = timeStampToken.getSID();
		BigInteger signerCertSerialNumber = signerId.getSerialNumber();
		X500Principal signerCertIssuer = signerId.getIssuer();
		X509Certificate tsaCertificate = null;
		for (Certificate certificate : certificates) {
			X509Certificate x509Certificate = (X509Certificate) certificate;
			if (signerCertIssuer.equals(x509Certificate
					.getIssuerX500Principal())
					&& signerCertSerialNumber.equals(x509Certificate
							.getSerialNumber())) {
				tsaCertificate = x509Certificate;
				break;
			}
		}
		if (null == tsaCertificate) {
			throw new SecurityException("TSA cert not present in TST");
		}
		if (false == tsaCertificate.equals(certificateChain.get(0))) {
			throw new SecurityException("TST signing certificate mismatch");
		}

		/*
		 * Perform PKI validation via eID Trust Service.
		 */
		getXkms2Client().validate(tsaTrustDomain, certificateChain,
				validationDate, ocspResponses, crls);
	}
}
