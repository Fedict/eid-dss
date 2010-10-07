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

package be.fedict.eid.dss.spi;

import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Holds information about a signature.
 * 
 * @author Frank Cornelis
 * 
 */
public class SignatureInfo implements Serializable {

	private static final long serialVersionUID = 1L;

	private final X509Certificate signer;

	private final Date signingTime;

	private final String role;

	private final String firstName;

	private final String name;

	private final String middleName;

	private final Gender gender;

	private final byte[] photo;

	public SignatureInfo(X509Certificate signer, Date signingTime, String role,
			String firstName, String name, String middleName, Gender gender,
			byte[] photo) {
		this.signer = signer;
		this.signingTime = signingTime;
		this.role = role;

		this.firstName = firstName;
		this.name = name;
		this.middleName = middleName;
		this.gender = gender;
		this.photo = photo;
	}

	public SignatureInfo(X509Certificate signer, Date signingTime, String role) {
		this(signer, signingTime, role, null, null, null, null, null);
	}

	public SignatureInfo(X509Certificate signer, Date signingTime) {
		this(signer, signingTime, null);
	}

	public X509Certificate getSigner() {
		return this.signer;
	}

	public Date getSigningTime() {
		return this.signingTime;
	}

	public String getRole() {
		return this.role;
	}

	public String getFirstName() {
		return this.firstName;
	}

	public String getName() {
		return this.name;
	}

	public String getMiddleName() {
		return this.middleName;
	}

	public Gender getGender() {
		return this.gender;
	}

	public byte[] getPhoto() {
		return this.photo;
	}

	public static enum Gender {
		MALE, FEMALE
	}
}
