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

package test.unit.be.fedict.eid.dss.document.ooxml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import be.fedict.eid.applet.service.signer.ooxml.OOXMLProvider;
import be.fedict.eid.dss.document.ooxml.OOXMLDSSDocumentService;
import be.fedict.eid.dss.spi.SignatureInfo;

public class OOXMLDSSDocumentServiceTest {

	private static final Log LOG = LogFactory
			.getLog(OOXMLDSSDocumentServiceTest.class);

	@BeforeClass
	public static void setUp() {
		OOXMLProvider.install();
	}

	@Test
	public void testVerifySignatures() throws Exception {
		// setup
		OOXMLDSSDocumentService testedInstance = new OOXMLDSSDocumentService();
		byte[] document = IOUtils.toByteArray(OOXMLDSSDocumentServiceTest.class
				.getResourceAsStream("/hello-world-signed.docx"));

		// operate
		List<SignatureInfo> signatureInfos = testedInstance
				.verifySignatures(document);

		// verify
		assertNotNull(signatureInfos);
		assertEquals(1, signatureInfos.size());
		SignatureInfo signatureInfo = signatureInfos.get(0);
		assertNotNull(signatureInfo.getSigner());
		assertNotNull(signatureInfo.getSigningTime());
		LOG.debug("signing time: " + signatureInfo.getSigningTime());
	}
}
