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

import be.fedict.eid.dss.document.xml.XMLDSSDocumentService;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.SignatureInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Constants;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.*;

public class XMLDSSDocumentServiceLargeTest {

	private static final Log LOG = LogFactory
			.getLog(XMLDSSDocumentServiceLargeTest.class);

	@BeforeClass
	public static void setUp() {
		if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
			Security.addProvider(new BouncyCastleProvider());
		}
		//Init.init();
	}

//    @Test
    public void testGenerateLargeDocument() throws Exception {

        // setup
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
                .newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = documentBuilderFactory
                .newDocumentBuilder();
        Document document = documentBuilder.newDocument();
        Element rootElement = document.createElementNS("urn:test", "tns:root");
        rootElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:tns",
                "urn:test");
        document.appendChild(rootElement);
        Element dataElement = document.createElementNS("urn:test", "tns:data");
        rootElement.appendChild(dataElement);

        // add alot of nodes to test performance
        // when using xpath v1 in the co signature facet the c14n became really slow
        for (int i = 0; i < 80000; i++) {
            Element fooElement = document.createElementNS("urn:test", "tns:foo");
            fooElement.setTextContent("bar");
            dataElement.appendChild(fooElement);
        }

        // dump to file
        Source source = new DOMSource(document);
        File file = new File("large.xml");
        FileWriter fileWriter = new FileWriter(file);
        Result result = new StreamResult(fileWriter);
        TransformerFactory transformerFactory = TransformerFactory
                .newInstance();
        Transformer transformer = transformerFactory.newTransformer();
		/*
		 * We have to omit the ?xml declaration if we want to embed the
		 * document.
		 */
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(source, result);
        fileWriter.flush();
        fileWriter.close();
    }

	@Test
	public void testVerifyLargeSignedDocument() throws Exception {
		// setup
		InputStream signedDocumentInputStream = XMLDSSDocumentServiceLargeTest.class
				.getResourceAsStream("/large-signed.xml");
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
}
