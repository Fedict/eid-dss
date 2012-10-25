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
import java.security.Security;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.Enumeration;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sun.security.pkcs11.SunPKCS11;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.DSSIdentityConfig;
import be.fedict.eid.dss.model.KeyStoreType;
import be.fedict.eid.dss.model.exception.KeyStoreLoadException;

@Singleton
@Startup
public class IdentityServiceSingletonBean {

	private static final Log LOG = LogFactory
			.getLog(IdentityServiceSingletonBean.class);

	private PrivateKeyEntry identity;
	private DSSIdentityConfig identityConfig;

	@EJB
	private Configuration configuration;

	@PostConstruct
	public void init() {

		if (isIdentityConfigured()) {
			try {
				reloadIdentity();
			} catch (KeyStoreLoadException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * @return if an active identity is configured
	 */
	public boolean isIdentityConfigured() {
		return null != this.configuration.getValue(
				ConfigProperty.ACTIVE_IDENTITY, String.class);
	}

	/**
	 * @return list of all identity configurations's names
	 */
	public List<String> getIdentities() {

		return this.configuration.getIndexes(ConfigProperty.KEY_STORE_TYPE);
	}

	/**
	 * Set new active identity
	 * 
	 * @param name
	 *            new active identity's name
	 * @throws KeyStoreLoadException
	 *             failed to load keystore
	 */
	public void setActiveIdentity(String name) throws KeyStoreLoadException {

		LOG.debug("set active identity: " + name);
		DSSIdentityConfig dssIdentityConfig = findIdentityConfig(name);

		if (null == dssIdentityConfig) {
			throw new KeyStoreLoadException("Identity config \"" + name
					+ "\" not found!");
		}

		this.configuration.setValue(ConfigProperty.ACTIVE_IDENTITY, name);

		this.identity = loadIdentity(dssIdentityConfig);
		this.identityConfig = dssIdentityConfig;
		LOG.debug("private key entry reloaded");
	}

	/**
	 * Reload current active identity
	 * 
	 * @throws KeyStoreLoadException
	 *             failed to load keystore
	 */
	public void reloadIdentity() throws KeyStoreLoadException {

		DSSIdentityConfig dssIdentityConfig = findIdentityConfig(findActiveIdentityName());

		this.identity = loadIdentity(dssIdentityConfig);
		this.identityConfig = dssIdentityConfig;
		LOG.debug("private key entry reloaded");
	}

	/**
	 * Load identity keystore with specified name
	 * 
	 * @param name
	 *            identity name
	 * @return DSS identity
	 * @throws KeyStoreLoadException
	 *             failed to load keystore
	 */
	public PrivateKeyEntry loadIdentity(String name)
			throws KeyStoreLoadException {

		DSSIdentityConfig dssIdentityConfig = findIdentityConfig(name);
		return loadIdentity(dssIdentityConfig);
	}

	/**
	 * Load identity keystore
	 * 
	 * @param dssIdentityConfig
	 *            identity configuration
	 * @return private key entry of identity
	 * @throws KeyStoreLoadException
	 *             failed to load keystore
	 */
	public PrivateKeyEntry loadIdentity(DSSIdentityConfig dssIdentityConfig)
			throws KeyStoreLoadException {

		try {

			if (null == dssIdentityConfig) {
				throw new KeyStoreLoadException("Identity config is empty!");
			}

			FileInputStream keyStoreInputStream = null;
			if (dssIdentityConfig.getKeyStoreType().equals(KeyStoreType.PKCS11)) {
				Security.addProvider(new SunPKCS11(dssIdentityConfig
						.getKeyStorePath()));
			} else {
				try {
					keyStoreInputStream = new FileInputStream(
							dssIdentityConfig.getKeyStorePath());
				} catch (FileNotFoundException e) {
					throw new KeyStoreLoadException(
							"Can't load keystore from config-specified location: "
									+ dssIdentityConfig.getKeyStorePath(), e);
				}
			}

			// load keystore
			KeyStore keyStore = KeyStore.getInstance(dssIdentityConfig
					.getKeyStoreType().getJavaKeyStoreType());
			char[] password;
			if (null != dssIdentityConfig.getKeyStorePassword()
					&& !dssIdentityConfig.getKeyStorePassword().isEmpty()) {
				password = dssIdentityConfig.getKeyStorePassword()
						.toCharArray();
			} else {
				password = null;
			}
			keyStore.load(keyStoreInputStream, password);

			// find entry alias
			Enumeration<String> aliases = keyStore.aliases();
			if (!aliases.hasMoreElements()) {
				throw new KeyStoreLoadException("no keystore aliases present");
			}

			String alias;
			if (null != dssIdentityConfig.getKeyEntryAlias()
					&& !dssIdentityConfig.getKeyEntryAlias().trim().isEmpty()) {
				boolean found = false;
				while (aliases.hasMoreElements()) {
					if (aliases.nextElement().equals(
							dssIdentityConfig.getKeyEntryAlias())) {
						found = true;
						break;
					}
				}
				if (!found) {
					throw new KeyStoreLoadException(
							"no keystore entry with alias \""
									+ dssIdentityConfig.getKeyEntryAlias()
									+ "\"");
				}
				alias = dssIdentityConfig.getKeyEntryAlias();
			} else {
				alias = aliases.nextElement();
			}
			LOG.debug("keystore alias: " + alias);

			// get keystore entry
			char[] entryPassword;
			if (null != dssIdentityConfig.getKeyEntryPassword()
					&& !dssIdentityConfig.getKeyEntryPassword().isEmpty()) {
				entryPassword = dssIdentityConfig.getKeyEntryPassword()
						.toCharArray();
			} else {
				entryPassword = null;
			}

			KeyStore.Entry entry = keyStore.getEntry(alias,
					new KeyStore.PasswordProtection(entryPassword));
			if (!(entry instanceof PrivateKeyEntry)) {
				throw new KeyStoreLoadException("private key entry expected");
			}
			return (PrivateKeyEntry) entry;
		} catch (KeyStoreException e) {
			throw new KeyStoreLoadException(e);
		} catch (CertificateException e) {
			throw new KeyStoreLoadException(e);
		} catch (NoSuchAlgorithmException e) {
			throw new KeyStoreLoadException(e);
		} catch (UnrecoverableEntryException e) {
			throw new KeyStoreLoadException(e);
		} catch (IOException e) {
			throw new KeyStoreLoadException(e);
		}
	}

	/**
	 * @return current DSS Identity or <code>null</code> if none.
	 */
	public PrivateKeyEntry findIdentity() {

		// check identity config changed in dbase, if so reload!
		DSSIdentityConfig databaseIdentityConfig = findIdentityConfig();

		if (null != databaseIdentityConfig) {
			if (!databaseIdentityConfig.equals(this.identityConfig)) {
				try {
					this.identity = loadIdentity(databaseIdentityConfig);
				} catch (KeyStoreLoadException e) {
					throw new RuntimeException(e);
				}
				this.identityConfig = databaseIdentityConfig;
			}
		}
		return this.identity;
	}

	/**
	 * @return current identity's configuration or <codE>null</code> if none.
	 */
	public DSSIdentityConfig findIdentityConfig() {

		String activeIdentity = findActiveIdentityName();
		if (null == activeIdentity) {
			return null;
		}
		DSSIdentityConfig dssIdentityConfig = findIdentityConfig(activeIdentity);
		if (null == dssIdentityConfig) {
			throw new EJBException("Identity config " + activeIdentity
					+ " not found!");
		}
		return dssIdentityConfig;
	}

	/**
	 * @param name
	 *            identity name
	 * @return identity config or <code>null</code> if not found.
	 */
	public DSSIdentityConfig findIdentityConfig(String name) {

		KeyStoreType keyStoreType = this.configuration.getValue(
				ConfigProperty.KEY_STORE_TYPE, name, KeyStoreType.class);
		if (null == keyStoreType) {
			return null;
		}
		String keyStorePath = this.configuration.getValue(
				ConfigProperty.KEY_STORE_PATH, name, String.class);
		String keyStoreSecret = this.configuration.getValue(
				ConfigProperty.KEY_STORE_SECRET, name, String.class);
		String keyEntrySecret = this.configuration.getValue(
				ConfigProperty.KEY_ENTRY_SECRET, name, String.class);
		String keyEntryAlias = this.configuration.getValue(
				ConfigProperty.KEY_ENTRY_ALIAS, name, String.class);

		DSSIdentityConfig idPIdentityConfig = new DSSIdentityConfig(name,
				keyStoreType, keyStorePath, keyStoreSecret, keyEntrySecret,
				keyEntryAlias);

		String activeIdentity = findActiveIdentityName();
		if (null != activeIdentity) {
			idPIdentityConfig.setActive(idPIdentityConfig.getName().equals(
					activeIdentity));
		}

		return idPIdentityConfig;
	}

	private String findActiveIdentityName() {

		return this.configuration.getValue(ConfigProperty.ACTIVE_IDENTITY,
				String.class);
	}

	/**
	 * Add/update identity from specified configuration
	 * 
	 * @param dssIdentityConfig
	 *            identity configuration
	 * @return DSS Identity
	 * @throws KeyStoreLoadException
	 *             failed to load keystore
	 */
	public PrivateKeyEntry setIdentity(DSSIdentityConfig dssIdentityConfig)
			throws KeyStoreLoadException {

		LOG.debug("set identity: " + dssIdentityConfig.getName());

		this.configuration.setValue(ConfigProperty.KEY_STORE_TYPE,
				dssIdentityConfig.getName(),
				dssIdentityConfig.getKeyStoreType());
		this.configuration.setValue(ConfigProperty.KEY_STORE_PATH,
				dssIdentityConfig.getName(),
				dssIdentityConfig.getKeyStorePath());
		this.configuration.setValue(ConfigProperty.KEY_STORE_SECRET,
				dssIdentityConfig.getName(),
				dssIdentityConfig.getKeyStorePassword());
		this.configuration.setValue(ConfigProperty.KEY_ENTRY_SECRET,
				dssIdentityConfig.getName(),
				dssIdentityConfig.getKeyEntryPassword());
		if (null != dssIdentityConfig.getKeyEntryAlias()) {
			this.configuration.setValue(ConfigProperty.KEY_ENTRY_ALIAS,
					dssIdentityConfig.getName(),
					dssIdentityConfig.getKeyEntryAlias());
		}

		return loadIdentity(dssIdentityConfig.getName());
	}

	/**
	 * Remove identity configuration
	 * 
	 * @param name
	 *            name of identity config to remove
	 */
	public void removeIdentityConfig(String name) {

		LOG.debug("remove identity: " + name);

		String activeIdentity = findActiveIdentityName();
		if (null != activeIdentity && activeIdentity.equals(name)) {
			this.configuration.removeValue(ConfigProperty.ACTIVE_IDENTITY);
			this.identity = null;
			this.identityConfig = null;
		}

		this.configuration.removeValue(ConfigProperty.KEY_STORE_TYPE, name);
		this.configuration.removeValue(ConfigProperty.KEY_STORE_PATH, name);
		this.configuration.removeValue(ConfigProperty.KEY_STORE_SECRET, name);
		this.configuration.removeValue(ConfigProperty.KEY_ENTRY_SECRET, name);
		this.configuration.removeValue(ConfigProperty.KEY_ENTRY_ALIAS, name);
	}
}
