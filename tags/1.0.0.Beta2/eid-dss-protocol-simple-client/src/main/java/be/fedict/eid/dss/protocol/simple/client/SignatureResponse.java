/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009-2010 FedICT.
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

package be.fedict.eid.dss.protocol.simple.client;

import java.security.cert.X509Certificate;

/**
 * Signature response DTO.
 * 
 * @author Frank Cornelis
 * 
 */
public class SignatureResponse {

	private final byte[] decodedSignatureResponse;

	private final X509Certificate signatureCertificate;

	public SignatureResponse(byte[] decodedSignatureResponse,
			X509Certificate signatureCertificate) {
		this.decodedSignatureResponse = decodedSignatureResponse;
		this.signatureCertificate = signatureCertificate;
	}

	/**
	 * Gives back the signed document.
	 * 
	 * @return
	 */
	public byte[] getDecodedSignatureResponse() {
		return this.decodedSignatureResponse;
	}

	/**
	 * Gives back the X509 certificate of the signatory.
	 * 
	 * @return
	 */
	public X509Certificate getSignatureCertificate() {
		return this.signatureCertificate;
	}
}
