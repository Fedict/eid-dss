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
import javax.ejb.Stateless;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.model.DSSIdentityConfig;
import be.fedict.eid.dss.model.IdentityService;
import be.fedict.eid.dss.model.exception.KeyStoreLoadException;

@Stateless
public class IdentityServiceBean implements IdentityService {

	private static final Log LOG = LogFactory.getLog(IdentityServiceBean.class);

	@EJB
	private IdentityServiceSingletonBean identityServiceSingletonBean;

	/**
	 * {@inheritDoc}
	 */
	public void reloadIdentity() throws KeyStoreLoadException {

		this.identityServiceSingletonBean.reloadIdentity();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setActiveIdentity(String name) throws KeyStoreLoadException {

		this.identityServiceSingletonBean.setActiveIdentity(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean isIdentityConfigured() {

		return this.identityServiceSingletonBean.isIdentityConfigured();
	}

	/**
	 * {@inheritDoc}
	 */
	public List<String> getIdentities() {

		return this.identityServiceSingletonBean.getIdentities();
	}

	/**
	 * {@inheritDoc}
	 */
	public PrivateKeyEntry findIdentity() {
		return this.identityServiceSingletonBean.findIdentity();
	}

	/**
	 * {@inheritDoc}
	 */
	public PrivateKeyEntry setIdentity(DSSIdentityConfig dssIdentityConfig)
			throws KeyStoreLoadException {

		return this.identityServiceSingletonBean.setIdentity(dssIdentityConfig);
	}

	/**
	 * {@inheritDoc}
	 */
	public PrivateKeyEntry loadIdentity(DSSIdentityConfig dssIdentityConfig)
			throws KeyStoreLoadException {

		return this.identityServiceSingletonBean
				.loadIdentity(dssIdentityConfig);
	}

	/**
	 * {@inheritDoc}
	 */
	public DSSIdentityConfig findIdentityConfig() {

		return this.identityServiceSingletonBean.findIdentityConfig();
	}

	/**
	 * {@inheritDoc}
	 */
	public DSSIdentityConfig findIdentityConfig(String name) {

		return this.identityServiceSingletonBean.findIdentityConfig(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeIdentityConfig(String name) {

		this.identityServiceSingletonBean.removeIdentityConfig(name);
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIdentityFingerprint() {

		PrivateKeyEntry identity = findIdentity();
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

	/**
	 * {@inheritDoc}
	 */
	public List<X509Certificate> getIdentityCertificateChain() {

		PrivateKeyEntry identity = findIdentity();
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
