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

/**
 * Holds some of the OASIS DSS constants.
 * 
 * @author Frank Cornelis
 */
public class DSSConstants {

	private DSSConstants() {
		super();
	}

	public static final String RESULT_MAJOR_SUCCESS = "urn:oasis:names:tc:dss:1.0:resultmajor:Success";

	public static final String RESULT_MAJOR_REQUESTER_ERROR = "urn:oasis:names:tc:dss:1.0:resultmajor:RequesterError";

	public static final String RESULT_MINOR_VALID_SIGNATURE = "urn:oasis:names:tc:dss:1.0:resultminor:valid:signature:OnAllDocuments";

	public static final String RESULT_MINOR_VALID_MULTI_SIGNATURES = "urn:oasis:names:tc:dss:1.0:resultminor:ValidMultiSignatures";

	public static final String RESULT_MINOR_INVALID_SIGNATURE = "urn:oasis:names:tc:dss:1.0:resultminor:invalid:IncorrectSignature";

	public static final String RESULT_MINOR_NOT_PARSEABLE_XML_DOCUMENT = "urn:oasis:names:tc:dss:1.0:resultminor:NotParseableXMLDocument";

	public static final String RESULT_MINOR_NOT_SUPPORTED = "urn:oasis:names:tc:dss:1.0:resultminor:NotSupported";

	public static final String DSS_NAMESPACE = "urn:oasis:names:tc:dss:1.0:core:schema";

	public static final String VR_NAMESPACE = "urn:oasis:names:tc:dss-x:1.0:profiles:verificationreport:schema#";

	public static final String VR_RESULT_MAJOR_VALID = "urn:oasis:names:tc:dss:1.0:detail:valid";

	// artifact binding constants
	public static final String ARTIFACT_NAMESPACE = "be:fedict:eid:dss:profile:artifact-binding:1.0";

	public static final String RETURN_STORAGE_INFO = "ReturnStorageInfo";

	public static final String RETURN_STORED_DOCUMENT = "ReturnStoredDocument";

	// original document constants
	public static final String ORIGINAL_DOCUMENT_NAMESPACE = "be:fedict:eid:dss:profile:original-document:1.0";

	public static final String ORIGINAL_DOCUMENT_ELEMENT = "OriginalDocument";
}
