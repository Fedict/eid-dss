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

package test.unit.be.fedict.eid.dss.document.xml;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.Init;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;

import be.fedict.eid.dss.document.xml.XMLDSSDocumentService;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.SignatureInfo;

public class XMLDSSDocumentServiceTest {

	private static final Log LOG = LogFactory
			.getLog(XMLDSSDocumentServiceTest.class);

	@BeforeClass
	public static void setUp() {
		if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
			Security.addProvider(new BouncyCastleProvider());
		}
		//Init.init();
	}

	@Test
	public void testCheckIncomingDocumentWithoutNamespace() throws Exception {
		// setup
		XMLDSSDocumentService testedInstance = new XMLDSSDocumentService();
		byte[] document = "<test>hello world</test>".getBytes();

		// operate
		testedInstance.init(null, null);
		testedInstance.checkIncomingDocument(document);
	}

	@Test
	public void testCheckIncomingDocumentUnknownNamespace() throws Exception {
		// setup
		XMLDSSDocumentService testedInstance = new XMLDSSDocumentService();
		byte[] document = "<test xmlns=\"urn:test\">hello world</test>"
				.getBytes();
		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);

		// expectations
		EasyMock.expect(mockContext.getXmlSchema("urn:test")).andReturn(null);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(mockContext, null);
		testedInstance.checkIncomingDocument(document);

		// verify
		EasyMock.verify(mockContext);
	}

	@Test
	public void testCheckIncomingDocumentWithNamespaceChecking()
			throws Exception {
		// setup
		XMLDSSDocumentService testedInstance = new XMLDSSDocumentService();
		byte[] document = "<test xmlns=\"urn:test\">hello world</test>"
				.getBytes();
		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);

		byte[] xsd = IOUtils.toByteArray(XMLDSSDocumentServiceTest.class
				.getResourceAsStream("/test.xsd"));

		// expectations
		EasyMock.expect(mockContext.getXmlSchema("urn:test")).andReturn(xsd);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(mockContext, null);
		testedInstance.checkIncomingDocument(document);

		// verify
		EasyMock.verify(mockContext);
	}

	@Test
	public void testCheckIncomingDocumentWithNamespaceCheckingWithImporting()
			throws Exception {
		// setup
		XMLDSSDocumentService testedInstance = new XMLDSSDocumentService();
		byte[] document = ("<test2 xmlns=\"urn:test2\" xmlns:test=\"urn:test\">"
				+ "<test:test>hello world</test:test></test2>").getBytes();
		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);

		byte[] xsd2 = IOUtils.toByteArray(XMLDSSDocumentServiceTest.class
				.getResourceAsStream("/test-import.xsd"));
		byte[] xsd = IOUtils.toByteArray(XMLDSSDocumentServiceTest.class
				.getResourceAsStream("/test.xsd"));

		// expectations
		EasyMock.expect(mockContext.getXmlSchema("urn:test2")).andReturn(xsd2);
		EasyMock.expect(mockContext.getXmlSchema("urn:test")).andReturn(xsd);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(mockContext, null);
		testedInstance.checkIncomingDocument(document);

		// verify
		EasyMock.verify(mockContext);
	}

	@Test
	public void testVerifySignedDocument() throws Exception {
		// setup
		InputStream signedDocumentInputStream = XMLDSSDocumentServiceTest.class
				.getResourceAsStream("/signed-document.xml");
		byte[] signedDocument = IOUtils.toByteArray(signedDocumentInputStream);
		XMLDSSDocumentService testedInstance = new XMLDSSDocumentService();

		DSSDocumentContext mockDocumentContext = EasyMock
				.createMock(DSSDocumentContext.class);
		testedInstance.init(mockDocumentContext, "text/xml");

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
		List<SignatureInfo> result = testedInstance.verifySignatures(
				signedDocument, null);

		// verify
		EasyMock.verify(mockDocumentContext);
		assertNotNull(result);
		assertEquals(1, result.size());
		SignatureInfo signatureInfo = result.get(0);
		assertNotNull(signatureInfo.getSigner());
		LOG.debug("signer: "
				+ signatureInfo.getSigner().getSubjectX500Principal());
		assertTrue(signatureInfo.getSigner().getSubjectX500Principal()
				.toString().contains("Wim Vandenhaute"));
		assertNotNull(signatureInfo.getSigningTime());
		LOG.debug("signing time: " + signatureInfo.getSigningTime());
		LOG.debug("number of OCSPs: " + ocspResponsesCapture.getValue().size());
		LOG.debug("number of CRLs: " + crlsCapture.getValue().size());
		assertEquals(1, ocspResponsesCapture.getValue().size());
		assertEquals(1, crlsCapture.getValue().size());
		assertEquals(validationDateCapture.getValue(),
				signatureInfo.getSigningTime());

		assertEquals(2, tsaCrlsCapture.getValue().size());
	}

	@Test
	public void testVerifySignedDocumentWithStyleSheet() throws Exception {
		// setup
		InputStream signedDocumentInputStream = XMLDSSDocumentServiceTest.class
				.getResourceAsStream("/signed-with-stylesheet.xml");
		byte[] signedDocument = IOUtils.toByteArray(signedDocumentInputStream);
		XMLDSSDocumentService testedInstance = new XMLDSSDocumentService();

		DSSDocumentContext mockDocumentContext = EasyMock
				.createMock(DSSDocumentContext.class);
		testedInstance.init(mockDocumentContext, "text/xml");

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
		List<SignatureInfo> result = testedInstance.verifySignatures(
				signedDocument, null);

		// verify
		EasyMock.verify(mockDocumentContext);
		assertNotNull(result);
		assertEquals(1, result.size());
		SignatureInfo signatureInfo = result.get(0);
		assertNotNull(signatureInfo.getSigner());
		LOG.debug("signer: "
				+ signatureInfo.getSigner().getSubjectX500Principal());
		assertTrue(signatureInfo.getSigner().getSubjectX500Principal()
				.toString().contains("Frank Cornelis"));
		assertNotNull(signatureInfo.getSigningTime());
		LOG.debug("signing time: " + signatureInfo.getSigningTime());
		LOG.debug("number of OCSPs: " + ocspResponsesCapture.getValue().size());
		LOG.debug("number of CRLs: " + crlsCapture.getValue().size());
		assertEquals(1, ocspResponsesCapture.getValue().size());
		assertEquals(1, crlsCapture.getValue().size());
		assertEquals(validationDateCapture.getValue(),
				signatureInfo.getSigningTime());

		assertEquals(2, tsaCrlsCapture.getValue().size());
	}

	@Test
	public void testVerifySignedDocumentOriginalDocument() throws Exception {
		// setup
		InputStream signedDocumentInputStream = XMLDSSDocumentServiceTest.class
				.getResourceAsStream("/signed-document.xml");
		byte[] signedDocument = IOUtils.toByteArray(signedDocumentInputStream);

		InputStream originalDocumentInputStream = XMLDSSDocumentServiceTest.class
				.getResourceAsStream("/original-document.xml");
		byte[] originalDocument = IOUtils
				.toByteArray(originalDocumentInputStream);

		XMLDSSDocumentService testedInstance = new XMLDSSDocumentService();

		DSSDocumentContext mockDocumentContext = EasyMock
				.createMock(DSSDocumentContext.class);
		testedInstance.init(mockDocumentContext, "text/xml");

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
		List<SignatureInfo> result = testedInstance.verifySignatures(
				signedDocument, originalDocument);

		// verify
		EasyMock.verify(mockDocumentContext);
		assertNotNull(result);
		assertEquals(1, result.size());
		SignatureInfo signatureInfo = result.get(0);
		assertNotNull(signatureInfo.getSigner());
		LOG.debug("signer: "
				+ signatureInfo.getSigner().getSubjectX500Principal());
		assertTrue(signatureInfo.getSigner().getSubjectX500Principal()
				.toString().contains("Wim Vandenhaute"));
		assertNotNull(signatureInfo.getSigningTime());
		LOG.debug("signing time: " + signatureInfo.getSigningTime());
		LOG.debug("number of OCSPs: " + ocspResponsesCapture.getValue().size());
		LOG.debug("number of CRLs: " + crlsCapture.getValue().size());
		assertEquals(1, ocspResponsesCapture.getValue().size());
		assertEquals(1, crlsCapture.getValue().size());
		assertEquals(validationDateCapture.getValue(),
				signatureInfo.getSigningTime());

		assertEquals(2, tsaCrlsCapture.getValue().size());
	}

	@Test
	public void testVerifySignedDocumentOriginalDocumentChanged()
			throws Exception {
		// setup
		InputStream signedDocumentInputStream = XMLDSSDocumentServiceTest.class
				.getResourceAsStream("/signed-document.xml");
		byte[] signedDocument = IOUtils.toByteArray(signedDocumentInputStream);

		InputStream originalDocumentInputStream = XMLDSSDocumentServiceTest.class
				.getResourceAsStream("/changed-original-document.xml");
		byte[] originalDocument = IOUtils
				.toByteArray(originalDocumentInputStream);

		XMLDSSDocumentService testedInstance = new XMLDSSDocumentService();

		DSSDocumentContext mockDocumentContext = EasyMock
				.createMock(DSSDocumentContext.class);
		testedInstance.init(mockDocumentContext, "text/xml");

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
		try {
			testedInstance.verifySignatures(signedDocument, originalDocument);
			fail();
		} catch (RuntimeException e) {
			// expected
			assertEquals("not original document", e.getMessage());
		}
	}
}
