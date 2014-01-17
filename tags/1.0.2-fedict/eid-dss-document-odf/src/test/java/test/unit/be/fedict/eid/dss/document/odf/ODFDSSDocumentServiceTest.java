/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2011 FedICT.
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

package test.unit.be.fedict.eid.dss.document.odf;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;

import be.fedict.eid.dss.document.odf.ODFDSSDocumentService;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.SignatureInfo;

public class ODFDSSDocumentServiceTest {

	private static final Log LOG = LogFactory
			.getLog(ODFDSSDocumentServiceTest.class);

	@BeforeClass
	public static void setUp() throws Exception {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testVerification() throws Exception {
		// setup
		ODFDSSDocumentService testedInstance = new ODFDSSDocumentService();
		byte[] document = IOUtils.toByteArray(ODFDSSDocumentServiceTest.class
				.getResourceAsStream("/signed.odt"));

		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);
		Capture<List<X509Certificate>> certificateChainCapture = new Capture<List<X509Certificate>>();
		Capture<Date> validationDateCapture = new Capture<Date>();
		Capture<List<OCSPResp>> ocspResponsesCapture = new Capture<List<OCSPResp>>();
		Capture<List<X509CRL>> crlsCapture = new Capture<List<X509CRL>>();
		Capture<TimeStampToken> timeStampTokenCapture = new Capture<TimeStampToken>();
		mockContext.validate(EasyMock.capture(certificateChainCapture),
				EasyMock.capture(validationDateCapture),
				EasyMock.capture(ocspResponsesCapture),
				EasyMock.capture(crlsCapture));

		Capture<List<OCSPResp>> tsaOcspResponsesCapture = new Capture<List<OCSPResp>>();
		Capture<List<X509CRL>> tsaCrlsCapture = new Capture<List<X509CRL>>();
		mockContext.validate(EasyMock.capture(timeStampTokenCapture),
				EasyMock.capture(tsaOcspResponsesCapture),
				EasyMock.capture(tsaCrlsCapture));
		mockContext.validate(EasyMock.capture(timeStampTokenCapture),
				EasyMock.capture(tsaOcspResponsesCapture),
				EasyMock.capture(tsaCrlsCapture));

		expect(mockContext.getTimestampMaxOffset()).andReturn(17 * 1000L);
		expect(mockContext.getMaxGracePeriod()).andReturn(
				1000L * 60 * 60 * 24 * 7);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(mockContext,
				"application/vnd.oasis.opendocument.text");
		List<SignatureInfo> signatureInfos = testedInstance.verifySignatures(
				document, null);

		// verify
		EasyMock.verify(mockContext);
		assertNotNull(signatureInfos);
		assertEquals(1, signatureInfos.size());
		SignatureInfo signatureInfo = signatureInfos.get(0);
		assertNotNull(signatureInfo.getSigner());
		assertNotNull(signatureInfo.getSigningTime());
		LOG.debug("signing time: " + signatureInfo.getSigningTime());
		assertEquals(signatureInfo.getSigningTime(),
				validationDateCapture.getValue());
		assertEquals(signatureInfo.getSigner(), certificateChainCapture
				.getValue().get(0));
		assertEquals(1, ocspResponsesCapture.getValue().size());
		assertEquals(1, crlsCapture.getValue().size());
	}

	@Test
	public void testVerificationChangedODF() throws Exception {
		// setup
		ODFDSSDocumentService testedInstance = new ODFDSSDocumentService();
		byte[] document = IOUtils.toByteArray(ODFDSSDocumentServiceTest.class
				.getResourceAsStream("/signed-added.odt"));

		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(mockContext,
				"application/vnd.oasis.opendocument.text");

		try {
			testedInstance.verifySignatures(document, null);
			fail();
		} catch (RuntimeException e) {
			// verify
			EasyMock.verify(mockContext);
		}
	}
}
