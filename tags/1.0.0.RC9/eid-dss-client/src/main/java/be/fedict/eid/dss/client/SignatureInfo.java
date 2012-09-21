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

package be.fedict.eid.dss.client;

import java.security.cert.X509Certificate;
import java.util.Date;

/**
 * Signature information container class.
 * 
 * @author Frank Cornelis
 * 
 */
public class SignatureInfo {

	private final X509Certificate signer;

	private final Date signingTime;

	private final String role;

	/**
	 * Main constructor.
	 * 
	 * @param signer
	 * @param signingTime
	 * @param role
	 */
	public SignatureInfo(X509Certificate signer, Date signingTime, String role) {
		this.signer = signer;
		this.signingTime = signingTime;
		this.role = role;
	}

	/**
	 * Gives back the X509 certificate of the signatory.
	 * 
	 * @return the X509 certificate.
	 */
	public X509Certificate getSigner() {
		return this.signer;
	}

	/**
	 * Gives back the date/time the signature was created.
	 * 
	 * @return the signing time as date.
	 */
	public Date getSigningTime() {
		return this.signingTime;
	}

	/**
	 * Gives back the claimed role of the signatory.
	 * 
	 * @return the claimed role as string.
	 */
	public String getRole() {
		return this.role;
	}
}
