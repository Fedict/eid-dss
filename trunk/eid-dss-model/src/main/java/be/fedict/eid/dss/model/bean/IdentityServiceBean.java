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

import java.security.KeyStore.PrivateKeyEntry;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.model.IdentityService;

@Stateless
public class IdentityServiceBean implements IdentityService {

	private static final Log LOG = LogFactory.getLog(IdentityServiceBean.class);

	@EJB
	private IdentityServiceSingletonBean identityServiceSingletonBean;

	public void reloadIdentity() {
		try {
			this.identityServiceSingletonBean.reloadIdentity();
		} catch (Exception e) {
			throw new EJBException("could not reload the identity: "
					+ e.getMessage(), e);
		}
	}

	public PrivateKeyEntry getIdentity() {
		return this.identityServiceSingletonBean.getIdentity();
	}

	public String getIdentityFingerprint() {
		PrivateKeyEntry identity = getIdentity();
		if (null == identity) {
			return null;
		}
		X509Certificate certificate = (X509Certificate) identity
				.getCertificate();
		if (null == certificate) {
			return null;
		}
		String fingerprint;
		try {
			fingerprint = DigestUtils.shaHex(certificate.getEncoded());
		} catch (CertificateEncodingException e) {
			LOG.error("cert encoding error: " + e.getMessage(), e);
			return null;
		}
		return fingerprint;
	}

	public List<X509Certificate> getIdentityCertificateChain() {
		PrivateKeyEntry identity = getIdentity();
		List<X509Certificate> identityCertificateChain = new LinkedList<X509Certificate>();
		if (null == identity) {
			return identityCertificateChain;
		}
		Certificate[] certificateChain = identity.getCertificateChain();
		if (null == certificateChain) {
			return identityCertificateChain;
		}
		for (Certificate certificate : certificateChain) {
			identityCertificateChain.add((X509Certificate) certificate);
		}
		return identityCertificateChain;
	}
}
