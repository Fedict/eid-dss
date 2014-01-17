/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010-2011 FedICT.
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

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertStoreException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import javax.ejb.Local;

import org.bouncycastle.cms.CMSException;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;

import be.fedict.trust.client.XKMS2Client;
import be.fedict.trust.client.exception.RevocationDataNotFoundException;
import be.fedict.trust.client.exception.TrustDomainNotFoundException;
import be.fedict.trust.client.exception.ValidationFailedException;

/**
 * Interface for trust validation service component.
 * 
 * @author Frank Cornelis
 * 
 */
@Local
public interface TrustValidationService {

	void validate(List<X509Certificate> certificateChain, Date validationDate,
			List<OCSPResp> ocspResponses, List<X509CRL> crls)
			throws CertificateEncodingException, TrustDomainNotFoundException,
			RevocationDataNotFoundException, ValidationFailedException;

	void validate(TimeStampToken timeStampToken)
			throws CertificateEncodingException, ValidationFailedException,
			TrustDomainNotFoundException, RevocationDataNotFoundException;

	XKMS2Client getXkms2Client();

	/**
	 * Performs a historical trust validation on the given timestamp token.
	 * 
	 * @param timeStampToken
	 * @param ocspResponses
	 * @param crls
	 * @throws ValidationFailedException
	 * @throws RevocationDataNotFoundException
	 * @throws TrustDomainNotFoundException
	 * @throws CertificateEncodingException
	 * @throws CMSException
	 * @throws NoSuchProviderException
	 * @throws NoSuchAlgorithmException
	 * @throws CertStoreException
	 * @throws IOException 
	 */
	void validate(TimeStampToken timeStampToken, List<OCSPResp> ocspResponses,
			List<X509CRL> crls) throws CertificateEncodingException,
			TrustDomainNotFoundException, RevocationDataNotFoundException,
			ValidationFailedException, NoSuchAlgorithmException,
			NoSuchProviderException, CMSException, CertStoreException, IOException;
}
