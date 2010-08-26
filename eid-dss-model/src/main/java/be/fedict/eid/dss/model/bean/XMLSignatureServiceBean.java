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

import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.eid.applet.service.spi.CertificateSecurityException;
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.applet.service.spi.ExpiredCertificateSecurityException;
import be.fedict.eid.applet.service.spi.RevokedCertificateSecurityException;
import be.fedict.eid.applet.service.spi.SignatureService;
import be.fedict.eid.applet.service.spi.TrustCertificateSecurityException;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;

/**
 * XML Signature Service bean. Acts as a proxy towards the actual
 * SignatureService implementation.
 * 
 * @author Frank Cornelis
 * 
 */
@Stateless
@Local(SignatureService.class)
@LocalBinding(jndiBinding = "fedict/eid/dss/XMLSignatureServiceBean")
public class XMLSignatureServiceBean implements SignatureService {

	@EJB
	private Configuration configuration;

	public String getFilesDigestAlgorithm() {
		return null;
	}

	public DigestInfo preSign(List<DigestInfo> digestInfos,
			List<X509Certificate> signingCertificateChain)
			throws NoSuchAlgorithmException {
		XMLSignatureService signatureService = getXMLSignatureService();
		return signatureService.preSign(digestInfos, signingCertificateChain);
	}

	public void postSign(byte[] signatureValue,
			List<X509Certificate> signingCertificateChain)
			throws ExpiredCertificateSecurityException,
			RevokedCertificateSecurityException,
			TrustCertificateSecurityException, CertificateSecurityException,
			SecurityException {
		XMLSignatureService signatureService = getXMLSignatureService();
		signatureService.postSign(signatureValue, signingCertificateChain);
	}

	private XMLSignatureService getXMLSignatureService() {
		String tspUrl = this.configuration.getValue(ConfigProperty.TSP_URL,
				String.class);

		Boolean useHttpProxy = this.configuration.getValue(
				ConfigProperty.HTTP_PROXY_ENABLED, Boolean.class);
		String httpProxyHost;
		int httpProxyPort;
		if (null != useHttpProxy && true == useHttpProxy) {
			httpProxyHost = this.configuration.getValue(
					ConfigProperty.HTTP_PROXY_HOST, String.class);
			httpProxyPort = this.configuration.getValue(
					ConfigProperty.HTTP_PROXY_PORT, Integer.class);
		} else {
			httpProxyHost = null;
			httpProxyPort = 0;
		}

		String xkmsUrl = this.configuration.getValue(ConfigProperty.XKMS_URL,
				String.class);

		XMLSignatureService signatureService = new XMLSignatureService(tspUrl,
				httpProxyHost, httpProxyPort, xkmsUrl);
		return signatureService;
	}
}
