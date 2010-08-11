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
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.trust.client.XKMS2Client;

/**
 * Revocation data service implementation using the eID Trust Service.
 * 
 * @author Frank Cornelis
 * 
 */
public class TrustServiceRevocationDataService implements RevocationDataService {

	private final XKMS2Client xkms2Client;

	public TrustServiceRevocationDataService() {
		this.xkms2Client = new XKMS2Client(
				"http://localhost:8080/eid-trust-service-ws/xkms2");
	}

	public RevocationData getRevocationData(
			List<X509Certificate> certificateChain) {
		try {
			this.xkms2Client.validate("BE", certificateChain, true);
		} catch (Exception e) {
			throw new RuntimeException(
					"error validating signing certificate chain: "
							+ e.getMessage(), e);
		}
		RevocationValuesType revocationValues = this.xkms2Client
				.getRevocationValues();
		RevocationData revocationData = new RevocationData();
		CRLValuesType crlValues = revocationValues.getCRLValues();
		if (null != crlValues) {
			List<EncapsulatedPKIDataType> encapsulatedCRLValueList = crlValues
					.getEncapsulatedCRLValue();
			for (EncapsulatedPKIDataType encapsulatedCRLValue : encapsulatedCRLValueList) {
				// XXX: dirty work-around for eID Trust Service client double
				// base64 encoding issue.
				byte[] crl = Base64.decode(encapsulatedCRLValue.getValue());
				revocationData.addCRL(crl);
			}
		}
		OCSPValuesType ocspValues = revocationValues.getOCSPValues();
		if (null != ocspValues) {
			List<EncapsulatedPKIDataType> encapsulatedOCSPValueList = ocspValues
					.getEncapsulatedOCSPValue();
			for (EncapsulatedPKIDataType encapsulatedOCSPValue : encapsulatedOCSPValueList) {
				byte[] ocsp = Base64.decode(encapsulatedOCSPValue.getValue());
				revocationData.addOCSP(ocsp);
			}
		}
		return revocationData;
	}
}
