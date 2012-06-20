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

package be.fedict.eid.dss.model.bean;

import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.applet.service.signer.facets.RevocationData;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.spi.TrustCertificateSecurityException;
import be.fedict.trust.client.XKMS2Client;
import be.fedict.trust.client.exception.ValidationFailedException;
import be.fedict.trust.client.jaxb.xades132.CRLValuesType;
import be.fedict.trust.client.jaxb.xades132.EncapsulatedPKIDataType;
import be.fedict.trust.client.jaxb.xades132.OCSPValuesType;
import be.fedict.trust.client.jaxb.xades132.RevocationValuesType;

/**
 * Revocation data service implementation using the eID Trust Service.
 * 
 * @author Frank Cornelis
 */
public class TrustServiceRevocationDataService implements RevocationDataService {

	private static final Log LOG = LogFactory
			.getLog(TrustServiceRevocationDataService.class);

	private final XKMS2Client xkms2Client;

	private final String trustDomain;

	public TrustServiceRevocationDataService(XKMS2Client xkms2Client,
			String trustDomain) {
		this.xkms2Client = xkms2Client;
		this.trustDomain = trustDomain;
	}

	public RevocationData getRevocationData(
			List<X509Certificate> certificateChain) {
		LOG.debug("retrieving revocation data for: "
				+ certificateChain.get(0).getSubjectX500Principal());
		try {
			this.xkms2Client.validate(this.trustDomain, certificateChain, true);
		} catch (ValidationFailedException e) {
			throw new TrustCertificateSecurityException();
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
				byte[] crl = encapsulatedCRLValue.getValue();
				revocationData.addCRL(crl);
			}
		}
		OCSPValuesType ocspValues = revocationValues.getOCSPValues();
		if (null != ocspValues) {
			List<EncapsulatedPKIDataType> encapsulatedOCSPValueList = ocspValues
					.getEncapsulatedOCSPValue();
			for (EncapsulatedPKIDataType encapsulatedOCSPValue : encapsulatedOCSPValueList) {
				byte[] ocsp = encapsulatedOCSPValue.getValue();
				revocationData.addOCSP(ocsp);
			}
		}
		return revocationData;
	}
}
