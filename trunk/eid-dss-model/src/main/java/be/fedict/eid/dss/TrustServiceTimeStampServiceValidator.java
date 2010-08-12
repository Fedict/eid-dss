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

package be.fedict.eid.dss;

import java.security.cert.X509Certificate;
import java.util.List;

import org.bouncycastle.util.encoders.Base64;
import org.etsi.uri._01903.v1_3.CRLValuesType;
import org.etsi.uri._01903.v1_3.EncapsulatedPKIDataType;
import org.etsi.uri._01903.v1_3.OCSPValuesType;
import org.etsi.uri._01903.v1_3.RevocationValuesType;

import be.fedict.eid.applet.service.signer.facets.RevocationData;
import be.fedict.eid.applet.service.signer.facets.TimeStampServiceValidator;
import be.fedict.trust.client.XKMS2Client;

public class TrustServiceTimeStampServiceValidator implements
		TimeStampServiceValidator {

	private final XKMS2Client xkms2Client;

	public TrustServiceTimeStampServiceValidator() {
		this.xkms2Client = new XKMS2Client(
				"http://localhost:8080/eid-trust-service-ws/xkms2");
	}

	public void validate(List<X509Certificate> certificateChain,
			RevocationData revocationData) throws Exception {
		this.xkms2Client.validate("BE-TSA", certificateChain,
				revocationData != null);
		if (null == revocationData) {
			return;
		}
		RevocationValuesType revocationValues = this.xkms2Client
				.getRevocationValues();
		CRLValuesType crlValues = revocationValues.getCRLValues();
		if (null != crlValues) {
			List<EncapsulatedPKIDataType> encapsulatedCrls = crlValues
					.getEncapsulatedCRLValue();
			for (EncapsulatedPKIDataType encapsulatedCrl : encapsulatedCrls) {
				// XXX: stupid work-around for double base64 coding
				byte[] encodedCrl = Base64.decode(encapsulatedCrl.getValue());
				revocationData.addCRL(encodedCrl);
			}
		}
		OCSPValuesType ocspValues = revocationValues.getOCSPValues();
		if (null != ocspValues) {
			List<EncapsulatedPKIDataType> encapsulatedOcsps = ocspValues
					.getEncapsulatedOCSPValue();
			for (EncapsulatedPKIDataType encapsulatedOcsp : encapsulatedOcsps) {
				// XXX: stupid work-around for double base64 coding
				byte[] encodedOcsp = Base64.decode(encapsulatedOcsp.getValue());
				revocationData.addOCSP(encodedOcsp);
			}
		}
	}
}
