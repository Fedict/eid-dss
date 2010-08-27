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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Enumeration;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.KeyStoreType;

@Singleton
@Startup
public class IdentityServiceSingletonBean {

	private static final Log LOG = LogFactory
			.getLog(IdentityServiceSingletonBean.class);

	private PrivateKeyEntry identity;

	@EJB
	private Configuration configuration;

	@PostConstruct
	public void postConstruct() {
		try {
			reloadIdentity();
		} catch (KeyStoreException e) {
			throw new EJBException("error loading the service identity: "
					+ e.getMessage(), e);
		} catch (FileNotFoundException e) {
			throw new EJBException("error loading the service identity: "
					+ e.getMessage(), e);
		} catch (NoSuchAlgorithmException e) {
			throw new EJBException("error loading the service identity: "
					+ e.getMessage(), e);
		} catch (CertificateException e) {
			throw new EJBException("error loading the service identity: "
					+ e.getMessage(), e);
		} catch (IOException e) {
			throw new EJBException("error loading the service identity: "
					+ e.getMessage(), e);
		} catch (UnrecoverableEntryException e) {
			throw new EJBException("error loading the service identity: "
					+ e.getMessage(), e);
		}
	}

	public void reloadIdentity() throws KeyStoreException,
			NoSuchAlgorithmException, CertificateException, IOException,
			UnrecoverableEntryException {
		KeyStoreType keyStoreType = this.configuration.getValue(
				ConfigProperty.KEY_STORE_TYPE, KeyStoreType.class);
		String keyStorePath = this.configuration.getValue(
				ConfigProperty.KEY_STORE_PATH, String.class);
		String keyStoreSecret = this.configuration.getValue(
				ConfigProperty.KEY_STORE_SECRET, String.class);

		if (null == keyStoreType) {
			this.identity = null;
			return;
		}
		if (null == keyStorePath || keyStorePath.isEmpty()) {
			this.identity = null;
			return;
		}
		if (KeyStoreType.PKCS12 != keyStoreType
				&& KeyStoreType.JKS != keyStoreType) {
			throw new EJBException("unsupported keystore type: " + keyStoreType);
		}
		KeyStore keyStore = KeyStore.getInstance(keyStoreType
				.getJavaKeyStoreType());
		FileInputStream keyStoreInputStream = new FileInputStream(keyStorePath);
		char[] password;
		if (null != keyStoreSecret && !keyStoreSecret.isEmpty()) {
			password = keyStoreSecret.toCharArray();
		} else {
			password = null;
		}
		keyStore.load(keyStoreInputStream, password);
		Enumeration<String> aliases = keyStore.aliases();
		if (false == aliases.hasMoreElements()) {
			throw new EJBException("no keystore aliases present");
		}
		String alias = aliases.nextElement();
		LOG.debug("keystore alias: " + alias);
		KeyStore.Entry entry = keyStore.getEntry(alias,
				new KeyStore.PasswordProtection(password));
		if (false == entry instanceof KeyStore.PrivateKeyEntry) {
			throw new EJBException("private key entry expected");
		}
		KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;
		this.identity = privateKeyEntry;
		LOG.debug("private key entry reloaded");
	}

	public PrivateKeyEntry getIdentity() {
		return this.identity;
	}
}
