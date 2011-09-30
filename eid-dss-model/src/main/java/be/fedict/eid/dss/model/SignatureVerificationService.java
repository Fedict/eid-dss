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

package be.fedict.eid.dss.model;

import java.util.List;

import javax.ejb.Local;

import be.fedict.eid.dss.model.exception.DocumentFormatException;
import be.fedict.eid.dss.model.exception.InvalidSignatureException;
import be.fedict.eid.dss.spi.SignatureInfo;

/**
 * Service interface for signature verification service.
 * 
 * @author Frank Cornelis
 * 
 */
@Local
public interface SignatureVerificationService {

	/**
	 * Verifies the given document for the presence of valid signatures.
	 * 
	 * @param data
	 *            the given data.
	 * @param mimeType
	 *            the mime type of the given data.
	 * @param originalData
	 *            the optional original data.
	 * @return the list of all signing parties.
	 * @exception DocumentFormatException
	 *                in case the given data is not valid.
	 * @throws InvalidSignatureException
	 */
	List<SignatureInfo> verify(byte[] data, String mimeType, byte[] originalData)
			throws DocumentFormatException, InvalidSignatureException;
}
