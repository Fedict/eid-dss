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

package test.unit.be.fedict.eid.dss.document.asic;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import be.fedict.eid.dss.document.asic.ASiCDSSDocumentService;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.SignatureInfo;

public class ASiCDSSDocumentServiceTest {

	@Before
	public void setUp() {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testSignatureVerification() throws Exception {
		// setup
		InputStream documentInputStream = ASiCDSSDocumentServiceTest.class
				.getResourceAsStream("/signed.asice");
		byte[] document = IOUtils.toByteArray(documentInputStream);
		ASiCDSSDocumentService testedInstance = new ASiCDSSDocumentService();

		DSSDocumentContext mockDocumentContext = EasyMock
				.createMock(DSSDocumentContext.class);

		Capture<List<X509Certificate>> certificateChainCapture = new Capture<List<X509Certificate>>();
		Capture<Date> validationDateCapture = new Capture<Date>();
		Capture<List<OCSPResp>> ocspResponsesCapture = new Capture<List<OCSPResp>>();
		Capture<List<X509CRL>> crlsCapture = new Capture<List<X509CRL>>();
		mockDocumentContext.validate(EasyMock.capture(certificateChainCapture),
				EasyMock.capture(validationDateCapture),
				EasyMock.capture(ocspResponsesCapture),
				EasyMock.capture(crlsCapture));

		Capture<TimeStampToken> timeStampTokenCapture = new Capture<TimeStampToken>();
		Capture<List<OCSPResp>> tsaOcspResponsesCapture = new Capture<List<OCSPResp>>();
		Capture<List<X509CRL>> tsaCrlsCapture = new Capture<List<X509CRL>>();
		mockDocumentContext.validate(EasyMock.capture(timeStampTokenCapture),
				EasyMock.capture(tsaOcspResponsesCapture),
				EasyMock.capture(tsaCrlsCapture));
		mockDocumentContext.validate(EasyMock.capture(timeStampTokenCapture),
				EasyMock.capture(tsaOcspResponsesCapture),
				EasyMock.capture(tsaCrlsCapture));
		expect(mockDocumentContext.getTimestampMaxOffset()).andReturn(
				16 * 1000L);
		expect(mockDocumentContext.getMaxGracePeriod()).andReturn(
				1000L * 60 * 60 * 24 * 7);

		// prepare
		EasyMock.replay(mockDocumentContext);

		// operate
		testedInstance.init(mockDocumentContext,
				"application/vnd.etsi.asic-e+zip");
		List<SignatureInfo> result = testedInstance.verifySignatures(document,
				null);

		// verify
		EasyMock.verify(mockDocumentContext);
		assertNotNull(result);
		assertEquals(1, result.size());
	}

	@Test
	public void testSignatureVerificationChangedContainer() throws Exception {
		// setup
		InputStream documentInputStream = ASiCDSSDocumentServiceTest.class
				.getResourceAsStream("/signed-changed.asice");
		byte[] document = IOUtils.toByteArray(documentInputStream);
		ASiCDSSDocumentService testedInstance = new ASiCDSSDocumentService();

		DSSDocumentContext mockDocumentContext = EasyMock
				.createMock(DSSDocumentContext.class);

		// prepare
		EasyMock.replay(mockDocumentContext);

		// operate
		testedInstance.init(mockDocumentContext,
				"application/vnd.etsi.asic-e+zip");
		List<SignatureInfo> result = testedInstance.verifySignatures(document,
				null);

		// verify
		EasyMock.verify(mockDocumentContext);
		assertNotNull(result);
		assertEquals(0, result.size());
	}
}
