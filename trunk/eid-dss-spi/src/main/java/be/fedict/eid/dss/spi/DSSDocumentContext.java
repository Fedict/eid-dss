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
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import org.bouncycastle.ocsp.OCSPResp;

/**
 * Document context interface. Should only allow access to resources that are
 * not dependent on some proxy configuration.
 * 
 * @author Frank Cornelis
 * 
 */
public interface DSSDocumentContext extends Serializable {

	/**
	 * Gives back the XML schema for the given XML namespace.
	 * 
	 * @param namespace
	 * @return
	 */
	byte[] getXmlSchema(String namespace);

	/**
	 * Gives back the XML Style Sheet for the given XML namespace.
	 * 
	 * @param namespace
	 * @return
	 */
	byte[] getXmlStyleSheet(String namespace);

	/**
	 * Checks whether the given certificate chain is valid for the given
	 * validation time using the given revocation data.
	 * 
	 * @param certificateChain
	 * @param validationDate
	 * @param ocspResponses
	 * @param crls
	 * @throws Exception
	 */
	void validate(List<X509Certificate> certificateChain, Date validationDate,
			List<OCSPResp> ocspResponses, List<X509CRL> crls) throws Exception;
}
