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

import be.fedict.eid.dss.document.asic.ASiCDSSDocumentService;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.DocumentVisualization;
import be.fedict.eid.dss.spi.MimeType;
import be.fedict.eid.dss.spi.SignatureInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
import org.w3c.tidy.Tidy;

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

public class ASiCDSSDocumentServiceTest {

	private static final Log LOG = LogFactory
			.getLog(ASiCDSSDocumentServiceTest.class);

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

	@Test
	public void testVisualizationSignedASiC() throws Exception {
		// setup
		InputStream originalInputStream = ASiCDSSDocumentServiceTest.class
				.getResourceAsStream("/signed.asice");
		byte[] originalDocument = IOUtils.toByteArray(originalInputStream);
		ASiCDSSDocumentService testedInstance = new ASiCDSSDocumentService();

		// operate
		DocumentVisualization result = testedInstance.visualizeDocument(
				originalDocument, "en",new LinkedList<MimeType>(), "http://eid-dss.be/document?resource=");

		// verify
		assertNotNull(result);
		LOG.debug("browser content-type: " + result.getBrowserContentType());
		assertEquals("text/html;charset=utf-8", result.getBrowserContentType());
		String content = new String(result.getBrowserData());
		LOG.debug("content: " + content);
		Tidy tidy = new Tidy();
		Document document = tidy.parseDOM(
				new ByteArrayInputStream(result.getBrowserData()), null);
		Node filenameNode = XPathAPI.selectSingleNode(document,
				"//*[text() = 'helloworld.txt']");
		assertNotNull(filenameNode);
		Node signatureFilenameNode = XPathAPI.selectSingleNode(document,
				"//*[text() = 'META-INF/signatures.xml']");
		assertNull(signatureFilenameNode);
	}

	@Test
	public void testVisualizationSkipMetadataEntries() throws Exception {
		// setup
		InputStream originalInputStream = ASiCDSSDocumentServiceTest.class
				.getResourceAsStream("/visualization.asice");
		byte[] originalDocument = IOUtils.toByteArray(originalInputStream);
		ASiCDSSDocumentService testedInstance = new ASiCDSSDocumentService();

		// operate
		DocumentVisualization result = testedInstance.visualizeDocument(
				originalDocument, "en",new LinkedList<MimeType>(), "http://eid-dss.be/document?resource=");

		// verify
		assertNotNull(result);
		LOG.debug("browser content-type: " + result.getBrowserContentType());
		assertEquals("text/html;charset=utf-8", result.getBrowserContentType());
		String content = new String(result.getBrowserData());
		LOG.debug("content: " + content);
		Tidy tidy = new Tidy();
		Document document = tidy.parseDOM(
				new ByteArrayInputStream(result.getBrowserData()), null);
		Node filenameNode = XPathAPI.selectSingleNode(document,
				"//*[text() = 'helloworld.txt']");
		assertNotNull(filenameNode);
		Node signatureFilenameNode = XPathAPI.selectSingleNode(document,
				"//*[text() = 'META-INF/signatures.xml']");
		assertNull(signatureFilenameNode);
		assertNull(XPathAPI.selectSingleNode(document,
				"//*[text() = 'META-INF/container.xml']"));
		assertNull(XPathAPI.selectSingleNode(document,
				"//*[text() = 'META-INF/foobar-signatures-foobar.xml']"));
		assertNull(XPathAPI.selectSingleNode(document,
				"//*[text() = 'META-INF/manifest.xml']"));
		assertNull(XPathAPI.selectSingleNode(document,
				"//*[text() = 'META-INF/metadata.xml']"));
		assertNull(XPathAPI.selectSingleNode(document,
				"//*[text() = 'mimetype']"));
	}
}
