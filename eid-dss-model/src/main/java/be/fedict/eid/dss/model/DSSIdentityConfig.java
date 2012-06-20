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

import org.apache.commons.lang.builder.EqualsBuilder;

public class DSSIdentityConfig {

	private String name;
	private KeyStoreType keyStoreType;
	private String keyStorePath;
	private String keyStorePassword;
	private String keyEntryPassword;
	private String keyEntryAlias;

	private boolean active = false;

	public DSSIdentityConfig(String name) {

		this.name = name;
		this.keyStoreType = KeyStoreType.PKCS12;
	}

	public DSSIdentityConfig(String name, KeyStoreType keyStoreType,
			String keyStorePath, String keyStorePassword,
			String keyEntryPassword, String keyEntryAlias) {

		this.name = name;
		this.keyStoreType = keyStoreType;
		this.keyStorePath = keyStorePath;
		this.keyStorePassword = keyStorePassword;
		this.keyEntryPassword = keyEntryPassword;
		this.keyEntryAlias = keyEntryAlias;
	}

	public KeyStoreType getKeyStoreType() {
		return keyStoreType;
	}

	public void setKeyStoreType(KeyStoreType keyStoreType) {
		this.keyStoreType = keyStoreType;
	}

	public String getKeyStorePath() {
		return keyStorePath;
	}

	public void setKeyStorePath(String keyStorePath) {
		this.keyStorePath = keyStorePath;
	}

	public String getKeyStorePassword() {
		return keyStorePassword;
	}

	public void setKeyStorePassword(String keyStorePassword) {
		this.keyStorePassword = keyStorePassword;
	}

	public String getKeyEntryPassword() {
		return keyEntryPassword;
	}

	public void setKeyEntryPassword(String keyEntryPassword) {
		this.keyEntryPassword = keyEntryPassword;
	}

	public String getKeyEntryAlias() {
		return keyEntryAlias;
	}

	public void setKeyEntryAlias(String keyEntryAlias) {
		this.keyEntryAlias = keyEntryAlias;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DSSIdentityConfig)) {
			return false;
		}

		DSSIdentityConfig rhs = (DSSIdentityConfig) obj;
		return new EqualsBuilder().append(name, rhs.name)
				.append(keyStoreType, rhs.keyStoreType)
				.append(keyStorePath, rhs.keyStorePath)
				.append(keyStorePassword, rhs.keyStorePassword)
				.append(keyEntryPassword, rhs.keyEntryPassword)
				.append(keyEntryAlias, rhs.keyEntryAlias).isEquals();
	}
}
