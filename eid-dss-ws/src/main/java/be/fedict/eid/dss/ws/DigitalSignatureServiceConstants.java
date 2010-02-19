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

package be.fedict.eid.dss.ws;

public class DigitalSignatureServiceConstants {

	private DigitalSignatureServiceConstants() {
		super();
	}

	public static final String RESULT_MAJOR_SUCCESS = "urn:oasis:names:tc:dss:1.0:resultmajor:Success";

	public static final String RESULT_MAJOR_REQUESTER_ERROR = "urn:oasis:names:tc:dss:1.0:resultmajor:RequesterError";

	public static final String RESULT_MINOR_VALID_SIGNATURE = "urn:oasis:names:tc:dss:1.0:resultminor:valid:signature:OnAllDocuments";

	public static final String RESULT_MINOR_VALID_MULTI_SIGNATURES = "urn:oasis:names:tc:dss:1.0:resultminor:ValidMultiSignatures";

	public static final String RESULT_MINOR_INVALID_SIGNATURE = "urn:oasis:names:tc:dss:1.0:resultminor:invalid:IncorrectSignature";

	public static final String RESULT_MINOR_NOT_PARSEABLE_XML_DOCUMENT = "urn:oasis:names:tc:dss:1.0:resultminor:NotParseableXMLDocument";
}
