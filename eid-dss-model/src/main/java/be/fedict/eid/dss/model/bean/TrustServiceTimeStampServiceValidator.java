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

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.applet.service.signer.facets.RevocationData;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.dss.model.exception.TrustServiceClientException;
import be.fedict.trust.client.XKMS2Client;
import be.fedict.trust.client.exception.RevocationDataNotFoundException;
import be.fedict.trust.client.exception.TrustDomainNotFoundException;
import be.fedict.trust.client.exception.ValidationFailedException;
import be.fedict.trust.client.jaxb.xades132.CRLValuesType;
import be.fedict.trust.client.jaxb.xades132.EncapsulatedPKIDataType;
import be.fedict.trust.client.jaxb.xades132.OCSPValuesType;
import be.fedict.trust.client.jaxb.xades132.RevocationValuesType;

public class TrustServiceTimeStampServiceValidator implements
		TimeStampServiceValidator {

	private static final Log LOG = LogFactory
			.getLog(TrustServiceTimeStampServiceValidator.class);

	private final XKMS2Client xkms2Client;

	private final String trustDomain;

	public TrustServiceTimeStampServiceValidator(XKMS2Client xkms2Client,
			String trustDomain) {
		this.xkms2Client = xkms2Client;
		this.trustDomain = trustDomain;
	}

	public void validate(List<X509Certificate> certificateChain,
			RevocationData revocationData) throws Exception {
		LOG.debug("validating TSA certificate: "
				+ certificateChain.get(0).getSubjectX500Principal());
		try {
			this.xkms2Client.validate(this.trustDomain, certificateChain,
					revocationData != null);
		} catch (CertificateEncodingException e) {
			throw new TrustServiceClientException("certificate encoding error",
					e);
		} catch (TrustDomainNotFoundException e) {
			throw new TrustServiceClientException("trust domain not found", e);
		} catch (RevocationDataNotFoundException e) {
			throw new TrustServiceClientException("revocation data not found",
					e);
		} catch (ValidationFailedException e) {
			throw new TrustServiceClientException("validation failed", e);
		} catch (Exception e) {
			throw new TrustServiceClientException(
					"unknown trust service error", e);
		}
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
				byte[] encodedCrl = encapsulatedCrl.getValue();
				revocationData.addCRL(encodedCrl);
			}
		}
		OCSPValuesType ocspValues = revocationValues.getOCSPValues();
		if (null != ocspValues) {
			List<EncapsulatedPKIDataType> encapsulatedOcsps = ocspValues
					.getEncapsulatedOCSPValue();
			for (EncapsulatedPKIDataType encapsulatedOcsp : encapsulatedOcsps) {
				byte[] encodedOcsp = encapsulatedOcsp.getValue();
				revocationData.addOCSP(encodedOcsp);
			}
		}
	}
}
