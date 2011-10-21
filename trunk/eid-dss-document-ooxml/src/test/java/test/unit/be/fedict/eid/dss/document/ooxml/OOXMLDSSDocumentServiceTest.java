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

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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

import be.fedict.eid.applet.service.signer.ooxml.OOXMLProvider;
import be.fedict.eid.dss.document.ooxml.OOXMLDSSDocumentService;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.SignatureInfo;

public class OOXMLDSSDocumentServiceTest {

	private static final Log LOG = LogFactory
			.getLog(OOXMLDSSDocumentServiceTest.class);

	@BeforeClass
	public static void setUp() {
		if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
			Security.addProvider(new BouncyCastleProvider());
		}
		OOXMLProvider.install();
	}

	@Test
	public void testVerifySignatures() throws Exception {
		// setup
		OOXMLDSSDocumentService testedInstance = new OOXMLDSSDocumentService();
		byte[] document = IOUtils.toByteArray(OOXMLDSSDocumentServiceTest.class
				.getResourceAsStream("/hello-world-signed.docx"));

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
		testedInstance.init(mockContext, "mime-type");
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
	/*
	 * Something wrong with the digest calculation of SigAndRefsTimeStamp of
	 * Office2010. Fixed in Office2010 SP1.
	 */
	public void testVerifySignaturesOffice2011() throws Exception {
		// setup
		OOXMLDSSDocumentService testedInstance = new OOXMLDSSDocumentService();
		byte[] document = IOUtils.toByteArray(OOXMLDSSDocumentServiceTest.class
				.getResourceAsStream("/Office2010-SP1-XAdES-X-L.docx"));

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
		mockContext.validate(EasyMock.capture(timeStampTokenCapture));
		mockContext.validate(EasyMock.capture(timeStampTokenCapture));
		expect(mockContext.getTimestampMaxOffset()).andReturn(33 * 1000L);
		expect(mockContext.getMaxGracePeriod()).andReturn(
				1000L * 60 * 60 * 24 * 7);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(mockContext, "mime-type");
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
		for (X509Certificate certificate : certificateChainCapture.getValue()) {
			LOG.debug("certificate: " + certificate.getSubjectX500Principal());
		}
	}

	/**
	 * When you register the root-signed Belgium Root CA2 within Windows Trust
	 * Store, Office 2010 SP1 will use a certificate chain up to GlobalSign Root
	 * CA instead of the self-signed Belgium Root CA2.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testVerifySignaturesTest123Office() throws Exception {
		// setup
		OOXMLDSSDocumentService testedInstance = new OOXMLDSSDocumentService();
		byte[] document = IOUtils.toByteArray(OOXMLDSSDocumentServiceTest.class
				.getResourceAsStream("/Office2010-SP1-GlobalSign.docx"));

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
		mockContext.validate(EasyMock.capture(timeStampTokenCapture));
		mockContext.validate(EasyMock.capture(timeStampTokenCapture));
		expect(mockContext.getTimestampMaxOffset()).andReturn(33 * 1000L);
		expect(mockContext.getMaxGracePeriod()).andReturn(
				1000L * 60 * 60 * 24 * 7);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(mockContext, "mime-type");
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
		assertEquals(2, crlsCapture.getValue().size());
		List<X509CRL> crls = crlsCapture.getValue();
		for (X509CRL crl : crls) {
			LOG.debug("CRL: " + crl.getIssuerX500Principal());
		}
		assertEquals(4, certificateChainCapture.getValue().size());
		for (X509Certificate certificate : certificateChainCapture.getValue()) {
			LOG.debug("certificate: " + certificate.getSubjectX500Principal());
		}
	}
}
