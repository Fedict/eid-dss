/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010 FedICT.
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

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.ocsp.OCSPResp;

import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.TrustValidationService;
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

	public void validate(List<X509Certificate> certificateChain,
			Date validationDate, List<OCSPResp> ocspResponses,
			List<X509CRL> crls) throws CertificateEncodingException,
			TrustDomainNotFoundException, RevocationDataNotFoundException,
			ValidationFailedException {
		String xkmsUrl = this.configuration.getValue(ConfigProperty.XKMS_URL,
				String.class);
		XKMS2Client xkms2Client = new XKMS2Client(xkmsUrl);

		Boolean useHttpProxy = this.configuration.getValue(
				ConfigProperty.HTTP_PROXY_ENABLED, Boolean.class);
		if (null != useHttpProxy && true == useHttpProxy) {
			String httpProxyHost = this.configuration.getValue(
					ConfigProperty.HTTP_PROXY_HOST, String.class);
			int httpProxyPort = this.configuration.getValue(
					ConfigProperty.HTTP_PROXY_PORT, Integer.class);
			xkms2Client.setProxy(httpProxyHost, httpProxyPort);
		}

		String verifyTrustDomain = this.configuration.getValue(
				ConfigProperty.VERIFY_TRUST_DOMAIN, String.class);

		LOG.debug("validating certificate chain");
		LOG.warn("TODO: enable when eID Trust Service has been fixed");
		// TODO: enable when eID Trust Service has been fixed
		// xkms2Client.validate(verifyTrustDomain, certificateChain,
		// validationDate, ocspResponses, crls);
	}
}
