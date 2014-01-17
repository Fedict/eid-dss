/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009-2012 FedICT.
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

package test.integ.be.fedict.eid.dss.cxf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.List;

import javax.xml.ws.spi.Provider;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import be.fedict.eid.dss.client.DigitalSignatureServiceClient;
import be.fedict.eid.dss.client.SignatureInfo;

public class CXFTest {

	private static final Log LOG = LogFactory.getLog(CXFTest.class);

	@Test
	public void testProvider() {
		Provider provider = Provider.provider();
		LOG.debug("provider: " + provider.getClass().getName());
		assertEquals("org.apache.cxf.jaxws22.spi.ProviderImpl", provider
				.getClass().getName());
	}

	@Test
	public void testVerifyWithSigners() throws Exception {
		// setup
		InputStream signedDocumentInputStream = CXFTest.class
				.getResourceAsStream("/signed-document.xml");
		String signedDocument = IOUtils.toString(signedDocumentInputStream);

		String dssUrl = "https://www.e-contract.be/eid-dss-ws/dss";
		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient(
				dssUrl);
		client.setProxy("proxy.yourict.net", 8080);
		client.setLogging(true, true);

		// operate
		List<SignatureInfo> signers = client.verifyWithSigners(
				signedDocument.getBytes(), "text/xml",
				signedDocument.getBytes());

		// verify
		assertNotNull(signers);
		assertEquals(1, signers.size());
		SignatureInfo signatureInfo = signers.get(0);
		LOG.debug("signer: "
				+ signatureInfo.getSigner().getSubjectX500Principal());
		assertTrue(signatureInfo.getSigner().getSubjectX500Principal()
				.toString().contains("Frank Cornelis"));
		LOG.debug("signing time: " + signatureInfo.getSigningTime());
	}
}
