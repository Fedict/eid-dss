/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010-2012 FedICT.
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

import java.io.File;
import java.io.Serializable;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;

/**
 * Document context interface. Should only allow access to resources that are
 * not dependent on some proxy configuration.
 * 
 * @author Frank Cornelis
 */
public interface DSSDocumentContext extends Serializable {

	/**
	 * @param namespace
	 *            XML namespace
	 * @return the XML schema for the given XML namespace.
	 */
	byte[] getXmlSchema(String namespace);

	/**
	 * @param namespace
	 *            XML namespace
	 * @return the XML Style Sheet for the given XML namespace.
	 */
	byte[] getXmlStyleSheet(String namespace);

	/**
	 * Checks whether the given certificate chain is valid for the given
	 * validation time using the given revocation data.
	 * 
	 * @param certificateChain
	 *            certificate chain to validate
	 * @param validationDate
	 *            validation date
	 * @param ocspResponses
	 *            list of OCSP responses used in validation
	 * @param crls
	 *            list of CRLs used in validation
	 * @throws Exception
	 *             something went wrong
	 */
	void validate(List<X509Certificate> certificateChain, Date validationDate,
			List<OCSPResp> ocspResponses, List<X509CRL> crls) throws Exception;

	/**
	 * Validate the specified timestamp token. Validation time is now.
	 * 
	 * @param timeStampToken
	 *            the timestamp token to validate.
	 * @throws Exception
	 *             something went wrong
	 */
	void validate(TimeStampToken timeStampToken) throws Exception;

	/**
	 * Validate the given timestamp token. We perform a historical validation
	 * here using the given revocation data. The validation time equals the
	 * timestamp time.
	 * 
	 * @param timeStampToken
	 * @param ocspResponses
	 * @param crls
	 * @throws Exception
	 */
	void validate(TimeStampToken timeStampToken, List<OCSPResp> ocspResponses,
			List<X509CRL> crls) throws Exception;

	/**
	 * Used when validating timestamp tokens.
	 * 
	 * @return the maximum offset (in ms) for timestamps.
	 */
	Long getTimestampMaxOffset();

	Long getMaxGracePeriod();

	/**
	 * Via this method the {@link DSSDocumentService} can register files which
	 * should be deleted when the corresponding HTTP session is being destroyed.
	 * This ensures proper cleanup of all temporary files used by the eID DSS
	 * even in the case of unexpected behavior. No error will occur when the
	 * file already has been deleted.
	 * 
	 * @param tmpFile
	 *            the file to be deleted when the session is being destroyed.
	 */
	void deleteWhenSessionDestroyed(File tmpFile);
}
