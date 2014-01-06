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

package be.fedict.eid.dss.model;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.Local;

import be.fedict.eid.dss.model.exception.KeyStoreLoadException;

/**
 * Interface for the identity service. The identity service maintains the
 * identity of the eID DSS service.
 * 
 * @author Frank Cornelis
 */
@Local
public interface IdentityService {

	/**
	 * Reload the currently configured identity
	 * 
	 * @throws KeyStoreLoadException
	 *             failed to load keystore
	 */
	void reloadIdentity() throws KeyStoreLoadException;

	/**
	 * Sets specified identity as the active eID IdP Identity
	 * 
	 * @param name
	 *            name of the identity to become active
	 * @throws KeyStoreLoadException
	 *             failed to load the identity.
	 */
	void setActiveIdentity(String name) throws KeyStoreLoadException;

	/**
	 * Update/add an eID IdP Identity
	 * 
	 * @param dssIdentityConfig
	 *            the identity configuration
	 * @return the identity
	 * @throws KeyStoreLoadException
	 *             failed to load the identity.
	 */
	KeyStore.PrivateKeyEntry setIdentity(DSSIdentityConfig dssIdentityConfig)
			throws KeyStoreLoadException;

	/**
	 * Test if specified IdP Identity configuration is valid.
	 * 
	 * @param dssIdentityConfig
	 *            the identity configuration
	 * @return the identity
	 * @throws KeyStoreLoadException
	 *             failed to load the identity.
	 */
	KeyStore.PrivateKeyEntry loadIdentity(DSSIdentityConfig dssIdentityConfig)
			throws KeyStoreLoadException;

	/**
	 * @return the currently active eID IdP Identity config or <code>null</code>
	 *         if none is active
	 */
	DSSIdentityConfig findIdentityConfig();

	/**
	 * @param name
	 *            identity's name
	 * @return the identity config or <code>null</code> if not found.
	 */
	DSSIdentityConfig findIdentityConfig(String name);

	/**
	 * Remove specified identity configuration
	 * 
	 * @param name
	 *            name of the identity config to be removed
	 */
	void removeIdentityConfig(String name);

	/**
	 * @return all configured identity names
	 */
	List<String> getIdentities();

	/**
	 * @return if the IdP's identity is configured or not.
	 */
	boolean isIdentityConfigured();

	/**
	 * @return the identity of this eID IdP system.
	 */
	KeyStore.PrivateKeyEntry findIdentity();

	String getIdentityFingerprint();

	List<X509Certificate> getIdentityCertificateChain();
}
