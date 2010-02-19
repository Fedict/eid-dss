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

package test.be.fedict.eid.dss;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Test;

import be.fedict.eid.dss.client.DigitalSignatureServiceClient;
import be.fedict.eid.dss.client.NotParseableXMLDocumentException;

public class DigitalSignatureServiceTest {

	@Test
	public void testVerifyUnsignedXMLDocument() throws Exception {
		// setup
		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient();

		// operate
		boolean result = client.verify("<test/>");

		// verify
		assertFalse(result);
	}

	@Test
	public void testVerifyNonXMLDocument() throws Exception {
		// setup
		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient();

		// operate & verify
		try {
			client.verify("foo-bar");
			fail();
		} catch (NotParseableXMLDocumentException e) {
			// expected
		}
	}
}
