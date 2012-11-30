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

package test.unit.be.fedict.eid.dss.document.zip;

import be.fedict.eid.dss.document.zip.ZIPDSSDocumentService;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.DocumentVisualization;
import be.fedict.eid.dss.spi.MimeType;
import be.fedict.eid.dss.spi.SignatureInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.Init;
import org.apache.xpath.XPathAPI;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.*;

public class ZIPDSSDocumentServiceTest {

	private static final Log LOG = LogFactory
			.getLog(ZIPDSSDocumentServiceTest.class);

	@Before
	public void setUp() throws Exception {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testVerifySignature() throws Exception {
		// setup
		//Init.init();
		InputStream documentInputStream = ZIPDSSDocumentServiceTest.class
				.getResourceAsStream("/signed.zip");
		byte[] document = IOUtils.toByteArray(documentInputStream);
		ZIPDSSDocumentService testedInstance = new ZIPDSSDocumentService();

		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);
		Capture<List<X509Certificate>> certificateChainCapture = new Capture<List<X509Certificate>>();
		Capture<Date> validationDateCapture = new Capture<Date>();
		Capture<List<OCSPResp>> ocspResponsesCapture = new Capture<List<OCSPResp>>();
		Capture<List<X509CRL>> crlsCapture = new Capture<List<X509CRL>>();
		mockContext.validate(EasyMock.capture(certificateChainCapture),
				EasyMock.capture(validationDateCapture),
				EasyMock.capture(ocspResponsesCapture),
				EasyMock.capture(crlsCapture));

		Capture<TimeStampToken> timeStampTokenCapture = new Capture<TimeStampToken>();
		Capture<List<OCSPResp>> tsaOcspResponsesCapture = new Capture<List<OCSPResp>>();
		Capture<List<X509CRL>> tsaCrlsCapture = new Capture<List<X509CRL>>();
		mockContext.validate(EasyMock.capture(timeStampTokenCapture),
				EasyMock.capture(tsaOcspResponsesCapture),
				EasyMock.capture(tsaCrlsCapture));
		mockContext.validate(EasyMock.capture(timeStampTokenCapture),
				EasyMock.capture(tsaOcspResponsesCapture),
				EasyMock.capture(tsaCrlsCapture));

		expect(mockContext.getTimestampMaxOffset()).andReturn(16 * 1000L);
		expect(mockContext.getMaxGracePeriod()).andReturn(
				1000L * 60 * 60 * 24 * 7);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(mockContext, "application/zip");
		testedInstance.verifySignatures(document, null);

		// verify
		EasyMock.verify(mockContext);
	}

	@Test
	public void testVerifySignatureEncoding() throws Exception {
		// setup
		InputStream documentInputStream = ZIPDSSDocumentServiceTest.class
				.getResourceAsStream("/signed-encoding.zip");
		byte[] document = IOUtils.toByteArray(documentInputStream);
		ZIPDSSDocumentService testedInstance = new ZIPDSSDocumentService();

		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);
		Capture<List<X509Certificate>> certificateChainCapture = new Capture<List<X509Certificate>>();
		Capture<Date> validationDateCapture = new Capture<Date>();
		Capture<List<OCSPResp>> ocspResponsesCapture = new Capture<List<OCSPResp>>();
		Capture<List<X509CRL>> crlsCapture = new Capture<List<X509CRL>>();
		mockContext.validate(EasyMock.capture(certificateChainCapture),
				EasyMock.capture(validationDateCapture),
				EasyMock.capture(ocspResponsesCapture),
				EasyMock.capture(crlsCapture));

		Capture<TimeStampToken> timeStampTokenCapture = new Capture<TimeStampToken>();
		Capture<List<OCSPResp>> tsaOcspResponsesCapture = new Capture<List<OCSPResp>>();
		Capture<List<X509CRL>> tsaCrlsCapture = new Capture<List<X509CRL>>();
		mockContext.validate(EasyMock.capture(timeStampTokenCapture),
				EasyMock.capture(tsaOcspResponsesCapture),
				EasyMock.capture(tsaCrlsCapture));
		mockContext.validate(EasyMock.capture(timeStampTokenCapture),
				EasyMock.capture(tsaOcspResponsesCapture),
				EasyMock.capture(tsaCrlsCapture));

		expect(mockContext.getTimestampMaxOffset()).andReturn(16 * 1000L);
		expect(mockContext.getMaxGracePeriod()).andReturn(
				1000L * 60 * 60 * 24 * 7);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(mockContext, "application/zip");
		testedInstance.verifySignatures(document, null);

		// verify
		EasyMock.verify(mockContext);
	}

	@Test
	public void testVerifySignatureChangedContainer() throws Exception {
		// setup
		InputStream documentInputStream = ZIPDSSDocumentServiceTest.class
				.getResourceAsStream("/signed-changed.zip");
		byte[] document = IOUtils.toByteArray(documentInputStream);
		ZIPDSSDocumentService testedInstance = new ZIPDSSDocumentService();

		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(mockContext, "application/zip");

		List<SignatureInfo> resultList = testedInstance.verifySignatures(
				document, null);

		// verify
		EasyMock.verify(mockContext);
		assertNotNull(resultList);
		assertTrue(resultList.isEmpty());
	}

	@Test
	public void testVerifySignatureOriginalDocument() throws Exception {
		// setup
		InputStream documentInputStream = ZIPDSSDocumentServiceTest.class
				.getResourceAsStream("/signed.zip");
		byte[] document = IOUtils.toByteArray(documentInputStream);

		InputStream originalDocumentInputStream = ZIPDSSDocumentServiceTest.class
				.getResourceAsStream("/original.zip");
		byte[] originalDocument = IOUtils
				.toByteArray(originalDocumentInputStream);

		ZIPDSSDocumentService testedInstance = new ZIPDSSDocumentService();

		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);
		Capture<List<X509Certificate>> certificateChainCapture = new Capture<List<X509Certificate>>();
		Capture<Date> validationDateCapture = new Capture<Date>();
		Capture<List<OCSPResp>> ocspResponsesCapture = new Capture<List<OCSPResp>>();
		Capture<List<X509CRL>> crlsCapture = new Capture<List<X509CRL>>();
		mockContext.validate(EasyMock.capture(certificateChainCapture),
				EasyMock.capture(validationDateCapture),
				EasyMock.capture(ocspResponsesCapture),
				EasyMock.capture(crlsCapture));

		Capture<TimeStampToken> timeStampTokenCapture = new Capture<TimeStampToken>();
		Capture<List<OCSPResp>> tsaOcspResponsesCapture = new Capture<List<OCSPResp>>();
		Capture<List<X509CRL>> tsaCrlsCapture = new Capture<List<X509CRL>>();
		mockContext.validate(EasyMock.capture(timeStampTokenCapture),
				EasyMock.capture(tsaOcspResponsesCapture),
				EasyMock.capture(tsaCrlsCapture));
		mockContext.validate(EasyMock.capture(timeStampTokenCapture),
				EasyMock.capture(tsaOcspResponsesCapture),
				EasyMock.capture(tsaCrlsCapture));

		expect(mockContext.getTimestampMaxOffset()).andReturn(16 * 1000L);
		expect(mockContext.getMaxGracePeriod()).andReturn(
				1000L * 60 * 60 * 24 * 7);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(mockContext, "application/zip");
		testedInstance.verifySignatures(document, originalDocument);

		// verify
		EasyMock.verify(mockContext);
	}

	@Test
	public void testVerifySignatureOriginalDocumentRemovedComparedWithOriginal()
			throws Exception {
		// setup
		InputStream documentInputStream = ZIPDSSDocumentServiceTest.class
				.getResourceAsStream("/signed.zip");
		byte[] document = IOUtils.toByteArray(documentInputStream);

		InputStream originalDocumentInputStream = ZIPDSSDocumentServiceTest.class
				.getResourceAsStream("/original-removed.zip");
		byte[] originalDocument = IOUtils
				.toByteArray(originalDocumentInputStream);

		ZIPDSSDocumentService testedInstance = new ZIPDSSDocumentService();

		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(mockContext, "application/zip");

		try {
			testedInstance.verifySignatures(document, originalDocument);
			fail();
		} catch (RuntimeException e) {
			// verify
			EasyMock.verify(mockContext);
		}
	}

	@Test
	public void testVerifySignatureOriginalDocumentAddedComparedWithOriginal()
			throws Exception {
		// setup
		InputStream documentInputStream = ZIPDSSDocumentServiceTest.class
				.getResourceAsStream("/signed.zip");
		byte[] document = IOUtils.toByteArray(documentInputStream);

		InputStream originalDocumentInputStream = ZIPDSSDocumentServiceTest.class
				.getResourceAsStream("/original-added.zip");
		byte[] originalDocument = IOUtils
				.toByteArray(originalDocumentInputStream);

		ZIPDSSDocumentService testedInstance = new ZIPDSSDocumentService();

		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(mockContext, "application/zip");

		try {
			testedInstance.verifySignatures(document, originalDocument);
			fail();
		} catch (RuntimeException e) {
			LOG.debug("expected error: " + e.getMessage(), e);
			// verify
			EasyMock.verify(mockContext);
		}
	}

	@Test
	public void testVerifySignatureOriginalDocumentChangedComparedWithOriginal()
			throws Exception {
		// setup
		InputStream documentInputStream = ZIPDSSDocumentServiceTest.class
				.getResourceAsStream("/signed.zip");
		byte[] document = IOUtils.toByteArray(documentInputStream);

		InputStream originalDocumentInputStream = ZIPDSSDocumentServiceTest.class
				.getResourceAsStream("/original-changed.zip");
		byte[] originalDocument = IOUtils
				.toByteArray(originalDocumentInputStream);

		ZIPDSSDocumentService testedInstance = new ZIPDSSDocumentService();

		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(mockContext, "application/zip");

		try {
			testedInstance.verifySignatures(document, originalDocument);
			fail();
		} catch (RuntimeException e) {
			// verify
			EasyMock.verify(mockContext);
		}
	}

	@Test
	public void testVisualizationUnsignedZIP() throws Exception {
		// setup
		InputStream originalInputStream = ZIPDSSDocumentServiceTest.class
				.getResourceAsStream("/original.zip");
		byte[] originalDocument = IOUtils.toByteArray(originalInputStream);
		ZIPDSSDocumentService testedInstance = new ZIPDSSDocumentService();

		// operate
		DocumentVisualization result = testedInstance.visualizeDocument(
				originalDocument, "en", new LinkedList<MimeType>(), "http://eid-dss.be/document?resource=");

		// verify
		assertNotNull(result);
		LOG.debug("browser content-type: " + result.getBrowserContentType());
		assertEquals("text/html;charset=utf-8", result.getBrowserContentType());
		String content = new String(result.getBrowserData());
		LOG.debug("content: " + content);
		//Tidy tidy = new Tidy();
		//Document document = tidy.parseDOM(
		//		new ByteArrayInputStream(result.getBrowserData()), null);
		//Node filenameNode = XPathAPI.selectSingleNode(document,
		//		"//*[text() = 'helloworld.txt']");
		//assertNotNull(filenameNode);
	}

	@Test
	public void testVisualizationSignedZIP() throws Exception {
		// setup
		InputStream originalInputStream = ZIPDSSDocumentServiceTest.class
				.getResourceAsStream("/signed.zip");
		byte[] originalDocument = IOUtils.toByteArray(originalInputStream);
		ZIPDSSDocumentService testedInstance = new ZIPDSSDocumentService();

		// operate
		DocumentVisualization result = testedInstance.visualizeDocument(
				originalDocument, "en",new LinkedList<MimeType>(), "http://eid-dss.be/document?resource=");

		// verify
		assertNotNull(result);
		LOG.debug("browser content-type: " + result.getBrowserContentType());
		assertEquals("text/html;charset=utf-8", result.getBrowserContentType());
		String content = new String(result.getBrowserData());
		LOG.debug("content: " + content);
		//Tidy tidy = new Tidy();
		//Document document = tidy.parseDOM(
		//		new ByteArrayInputStream(result.getBrowserData()), null);
		//Node filenameNode = XPathAPI.selectSingleNode(document,
		//		"//*[text() = 'helloworld.txt']");
		//assertNotNull(filenameNode);
		//Node signatureFilenameNode = XPathAPI.selectSingleNode(document,
		//		"//*[text() = 'META-INF/documentsignatures.xml']");
		//assertNull(signatureFilenameNode);
	}

	@Test
	public void testVisualizationEncodingZIP() throws Exception {
		// setup
		InputStream originalInputStream = ZIPDSSDocumentServiceTest.class
				.getResourceAsStream("/encoding.zip");
		byte[] originalDocument = IOUtils.toByteArray(originalInputStream);
		ZIPDSSDocumentService testedInstance = new ZIPDSSDocumentService();

		// operate
		DocumentVisualization result = testedInstance.visualizeDocument(
				originalDocument, "en",new LinkedList<MimeType>(), "http://eid-dss.be/document?resource=");

		// verify
		assertNotNull(result);
		LOG.debug("browser content-type: " + result.getBrowserContentType());
		assertEquals("text/html;charset=utf-8", result.getBrowserContentType());
		String content = new String(result.getBrowserData());
		LOG.debug("content: " + content);

	}
}
