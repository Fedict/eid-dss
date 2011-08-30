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
 */
public class SignatureResponse {

	private final byte[] decodedSignatureResponse;

	private final String signatureResponseId;

	private final X509Certificate signatureCertificate;

	public SignatureResponse(byte[] decodedSignatureResponse,
			String signatureResponseId, X509Certificate signatureCertificate) {

		this.decodedSignatureResponse = decodedSignatureResponse;
		this.signatureResponseId = signatureResponseId;
		this.signatureCertificate = signatureCertificate;
	}

	/**
	 * @return the signed document or <code>null</code> if signature response ID
	 *         is passed
	 */
	public byte[] getDecodedSignatureResponse() {
		return this.decodedSignatureResponse;
	}

	/**
	 * @return the X509 certificate of the signatory.
	 */
	public X509Certificate getSignatureCertificate() {
		return this.signatureCertificate;
	}

	/**
	 * @return the signature response ID or <code>null</code> case signature
	 *         response was not null.
	 */
	public String getSignatureResponseId() {
		return signatureResponseId;
	}
}
