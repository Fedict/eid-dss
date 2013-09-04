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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import javax.smartcardio.CardException;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignContext;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.KeyInfo;
import javax.xml.crypto.dsig.keyinfo.KeyInfoFactory;
import javax.xml.crypto.dsig.keyinfo.X509Data;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import sun.security.pkcs11.wrapper.PKCS11Exception;
import be.fedict.eid.applet.DiagnosticTests;
import be.fedict.eid.applet.Messages;
import be.fedict.eid.applet.Messages.MESSAGE_ID;
import be.fedict.eid.applet.Status;
import be.fedict.eid.applet.View;
import be.fedict.eid.applet.sc.PcscEid;
import be.fedict.eid.dss.client.DigitalSignatureServiceClient;
import be.fedict.eid.dss.client.NotParseableXMLDocumentException;
import be.fedict.eid.dss.client.SignatureInfo;
import be.fedict.eid.dss.client.StorageInfoDO;

public class DigitalSignatureServiceTest {

	private static final Log LOG = LogFactory
			.getLog(DigitalSignatureServiceTest.class);

	public static class TestView implements View {

		public void addDetailMessage(String detailMessage) {
			LOG.debug("detail: " + detailMessage);
		}

		public Component getParentComponent() {
			return null;
		}

		public boolean privacyQuestion(boolean includeAddress,
				boolean includePhoto, String identityDataUsage) {
			return false;
		}

		public void setStatusMessage(Status status, String statusMessage) {
			LOG.debug("status: [" + status + "]: " + statusMessage);
		}

		public void progressIndication(int max, int current) {
		}

		@Override
		public void addTestResult(DiagnosticTests diagnosticTest,
				boolean success, String description) {
		}

		@Override
		public void increaseProgress() {
		}

		@Override
		public void resetProgress(int max) {
		}

		@Override
		public void setProgressIndeterminate() {
		}

		@Override
		public void setStatusMessage(Status status, MESSAGE_ID messageId) {
		}
	}

	@Test
	public void testVerifyUnsignedXMLDocument() throws Exception {
		// setup
		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient();

		// operate
		boolean result = client.verify("<test/>".getBytes(), "text/xml");

		// verify
		assertFalse(result);
	}

	@Test
	public void testVerifyNonXMLDocument() throws Exception {
		// setup
		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient();

		// operate & verify
		try {
			client.verify("foo-bar".getBytes(), "text/xml");
			fail();
		} catch (NotParseableXMLDocumentException e) {
			// expected
		}
	}

	@Test
	public void testSignedDocument() throws Exception {
		// setup
		String documentStr = "<document><data id=\"id\">hello world</data></document>";
		Document document = loadDocument(documentStr);

		signDocument(document);

		String signedDocument = toString(document);
		LOG.debug("signed document: " + signedDocument);

		NodeList signatureNodeList = document.getElementsByTagNameNS(
				XMLSignature.XMLNS, "Signature");
		assertEquals(1, signatureNodeList.getLength());

		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient();

		// operate
		boolean result = client.verify(signedDocument.getBytes(), "text/xml");

		// verify
		assertTrue(result);
	}

	@Test
	public void testVerifyWithSigners() throws Exception {
		// setup
		InputStream signedDocumentInputStream = DigitalSignatureServiceTest.class
				.getResourceAsStream("/signed-document.xml");
		String signedDocument = IOUtils.toString(signedDocumentInputStream);

		String dssUrl = "https://www.e-contract.be/eid-dss-ws/dss";
		// String dssUrl = "http://localhost/eid-dss-ws/dss";
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

	@Test
	public void testVerifyZIPDocument() throws Exception {
		// setup
		InputStream documentInputStream = DigitalSignatureServiceTest.class
				.getResourceAsStream("/test.zip");
		assertNotNull(documentInputStream);
		byte[] document = IOUtils.toByteArray(documentInputStream);
		String dssUrl = "http://localhost/eid-dss-ws/dss";
		// String dssUrl = "https://www.e-contract.be/eid-dss-ws/dss";
		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient(
				dssUrl);
		// client.setProxy("proxy.yourict.net", 8080);
		client.setLogging(true, true);

		// operate
		List<SignatureInfo> signers = client.verifyWithSigners(document,
				"application/zip");

		// verify
		for (SignatureInfo signer : signers) {
			LOG.debug("signer: " + signer.getSigner().getSubjectX500Principal());
			LOG.debug("signing time: " + signer.getSigningTime());
		}
	}

	@Test
	public void testVerifyBigFile() throws Exception {
		// setup
		File file = new File(
				"/home/fcorneli/Downloads/glassfish-3.1.2.2.zip");
		InputStream documentInputStream = new FileInputStream(file);
		assertNotNull(documentInputStream);
		byte[] document = IOUtils.toByteArray(documentInputStream);
		String dssUrl = "https://dss-ws.services.belgium.be/eid-dss-ws/dss";
		//String dssUrl = "http://localhost/eid-dss-ws/dss";
		// String dssUrl = "https://www.e-contract.be/eid-dss-ws/dss";
		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient(
				dssUrl);
		client.setProxy("proxy.yourict.net", 8080);
		//client.setLogging(true, true);

		// operate
		List<SignatureInfo> signers = client.verifyWithSigners(document,
				"application/zip");

		// verify
		for (SignatureInfo signer : signers) {
			LOG.debug("signer: " + signer.getSigner().getSubjectX500Principal());
			LOG.debug("signing time: " + signer.getSigningTime());
		}
	}

	@Test
	public void testVerifyOOXMLDocument() throws Exception {
		// setup
		InputStream documentInputStream = DigitalSignatureServiceTest.class
				.getResourceAsStream("/hello-world-signed.docx");
		assertNotNull(documentInputStream);
		byte[] document = IOUtils.toByteArray(documentInputStream);
		String dssUrl = "http://localhost/eid-dss-ws/dss";
		// String dssUrl = "https://www.e-contract.be/eid-dss-ws/dss";
		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient(
				dssUrl);
		// client.setProxy("proxy.yourict.net", 8080);
		client.setLogging(true, true);

		// operate
		List<SignatureInfo> signers = client
				.verifyWithSigners(document,
						"application/vnd.openxmlformats-officedocument.wordprocessingml.document");

		// verify
		for (SignatureInfo signer : signers) {
			LOG.debug("signer: " + signer.getSigner().getSubjectX500Principal());
			LOG.debug("signing time: " + signer.getSigningTime());
		}
	}

	@Test
	public void testVerifyODFDocument() throws Exception {
		// setup
		InputStream documentInputStream = DigitalSignatureServiceTest.class
				.getResourceAsStream("/signed.odt");
		assertNotNull(documentInputStream);
		byte[] document = IOUtils.toByteArray(documentInputStream);
		String dssUrl = "http://localhost/eid-dss-ws/dss";
		// String dssUrl = "https://www.e-contract.be/eid-dss-ws/dss";
		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient(
				dssUrl);
		// client.setProxy("proxy.yourict.net", 8080);
		client.setLogging(true, true);

		// operate
		List<SignatureInfo> signers = client.verifyWithSigners(document,
				"application/vnd.oasis.opendocument.text");

		// verify
		for (SignatureInfo signer : signers) {
			LOG.debug("signer: " + signer.getSigner().getSubjectX500Principal());
			LOG.debug("signing time: " + signer.getSigningTime());
		}
	}

	@Test
	public void testVerifyASiCDocument() throws Exception {
		// setup
		InputStream documentInputStream = DigitalSignatureServiceTest.class
				.getResourceAsStream("/signed.asice");
		assertNotNull(documentInputStream);
		byte[] document = IOUtils.toByteArray(documentInputStream);
		String dssUrl = "http://localhost/eid-dss-ws/dss";
		// String dssUrl = "https://www.e-contract.be/eid-dss-ws/dss";
		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient(
				dssUrl);
		// client.setProxy("proxy.yourict.net", 8080);
		client.setLogging(true, true);

		// operate
		List<SignatureInfo> signers = client.verifyWithSigners(document,
				"application/vnd.etsi.asic-e+zip");

		// verify
		for (SignatureInfo signer : signers) {
			LOG.debug("signer: " + signer.getSigner().getSubjectX500Principal());
			LOG.debug("signing time: " + signer.getSigningTime());
		}
	}

	@Test
	public void testVerifyChangedOriginalDocument() throws Exception {
		// setup
		InputStream signedDocumentInputStream = DigitalSignatureServiceTest.class
				.getResourceAsStream("/signed-document.xml");
		String signedDocument = IOUtils.toString(signedDocumentInputStream);

		InputStream fakeOriginalDocumentInputStream = DigitalSignatureServiceTest.class
				.getResourceAsStream("/fake-original-document.xml");
		byte[] fakeOriginalDocument = IOUtils
				.toByteArray(fakeOriginalDocumentInputStream);

		// String dssUrl = "https://www.e-contract.be/eid-dss-ws/dss";
		String dssUrl = "http://localhost/eid-dss-ws/dss";
		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient(
				dssUrl);
		// client.setProxy("proxy.yourict.net", 8080);
		client.setLogging(true, true);

		// operate
		try {
			client.verifyWithSigners(signedDocument.getBytes(), "text/xml",
					fakeOriginalDocument);
			fail();
		} catch (RuntimeException e) {
			// expected
		}
	}

	@Test
	public void testClaimedRole() throws Exception {
		// setup
		InputStream signedDocumentInputStream = DigitalSignatureServiceTest.class
				.getResourceAsStream("/example-xades-claimed-role.xml");
		String signedDocument = IOUtils.toString(signedDocumentInputStream);

		// String dssUrl = "https://www.e-contract.be/eid-dss-ws/dss";
		String dssUrl = "http://localhost/eid-dss-ws/dss";
		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient(
				dssUrl);
		client.setLogging(true, false);
		// client.setProxy("proxy.yourict.net", 8080);

		// operate
		List<SignatureInfo> signers = client.verifyWithSigners(
				signedDocument.getBytes(), "text/xml");

		// verify
		assertNotNull(signers);
		assertEquals(1, signers.size());
		SignatureInfo signatureInfo = signers.get(0);
		LOG.debug("signer: "
				+ signatureInfo.getSigner().getSubjectX500Principal());
		assertTrue(signatureInfo.getSigner().getSubjectX500Principal()
				.toString().contains("Frank Cornelis"));
		LOG.debug("signing time: " + signatureInfo.getSigningTime());
		LOG.debug("role: " + signatureInfo.getRole());
		assertEquals("eID Architect", signatureInfo.getRole());
	}

	@Test
	public void testArtifactBindingWebService() throws Exception {
		// String dssUrl = "https://www.e-contract.be/eid-dss-ws/dss";
		String dssUrl = "http://localhost/eid-dss-ws/dss";
		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient(
				dssUrl);
		// client.setProxy("proxy.yourict.net", 8080);
		client.setLogging(true, false);

		String documentContent = "Hello World";
		StorageInfoDO storageInfo = client.store(documentContent.getBytes(),
				"text/plain");
		LOG.debug("storage info artifact Id: " + storageInfo.getArtifact());
		LOG.debug("storage info not before: " + storageInfo.getNotBefore());
		LOG.debug("storage info not after: " + storageInfo.getNotAfter());

		byte[] resultDocument = client.retrieve(storageInfo.getArtifact());
		assertEquals(documentContent, new String(resultDocument));

		try {
			client.retrieve(storageInfo.getArtifact());
			fail();
		} catch (RuntimeException e) {
			// expected
		}
	}

	private void signDocument(Document document) throws IOException,
			PKCS11Exception, InterruptedException, NoSuchFieldException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, KeyStoreException, NoSuchAlgorithmException,
			CertificateException, UnrecoverableEntryException,
			InvalidAlgorithmParameterException, MarshalException,
			XMLSignatureException, CardException {
		Messages messages = new Messages(Locale.getDefault());
		PcscEid pcscEid = new PcscEid(new TestView(), messages);
		if (false == pcscEid.isEidPresent()) {
			LOG.debug("insert eID...");
			pcscEid.waitForEidPresent();
		}
		// PrivateKeyEntry privateKeyEntry = pcscEid.getPrivateKeyEntry();
		PrivateKeyEntry privateKeyEntry = null;
		// TODO: refactor once Commons eID has been released.

		XMLSignatureFactory signatureFactory = XMLSignatureFactory
				.getInstance("DOM");
		XMLSignContext signContext = new DOMSignContext(
				privateKeyEntry.getPrivateKey(), document.getDocumentElement());
		signContext.putNamespacePrefix(
				javax.xml.crypto.dsig.XMLSignature.XMLNS, "ds");

		DigestMethod digestMethod = signatureFactory.newDigestMethod(
				DigestMethod.SHA1, null);
		Reference reference = signatureFactory
				.newReference("#id", digestMethod);
		SignatureMethod signatureMethod = signatureFactory.newSignatureMethod(
				SignatureMethod.RSA_SHA1, null);
		CanonicalizationMethod canonicalizationMethod = signatureFactory
				.newCanonicalizationMethod(
						CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS,
						(C14NMethodParameterSpec) null);
		SignedInfo signedInfo = signatureFactory.newSignedInfo(
				canonicalizationMethod, signatureMethod,
				Collections.singletonList(reference));
		KeyInfoFactory keyInfoFactory = KeyInfoFactory.getInstance();
		List<Object> x509DataObjects = new LinkedList<Object>();
		X509Certificate signingCertificate = (X509Certificate) privateKeyEntry
				.getCertificate();
		x509DataObjects.add(signingCertificate);
		X509Data x509Data = keyInfoFactory.newX509Data(x509DataObjects);
		List<Object> keyInfoContent = new LinkedList<Object>();
		keyInfoContent.add(x509Data);
		KeyInfo keyInfo = keyInfoFactory.newKeyInfo(keyInfoContent);
		javax.xml.crypto.dsig.XMLSignature xmlSignature = signatureFactory
				.newXMLSignature(signedInfo, keyInfo);
		xmlSignature.sign(signContext);

		pcscEid.close();
	}

	private Document loadDocument(InputStream documentInputStream)
			throws ParserConfigurationException, SAXException, IOException {
		InputSource inputSource = new InputSource(documentInputStream);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.parse(inputSource);
		return document;
	}

	private Document loadDocument(String document)
			throws ParserConfigurationException, SAXException, IOException {
		InputStream documentInputStream = new ByteArrayInputStream(
				document.getBytes());
		return loadDocument(documentInputStream);
	}

	static String toString(Node dom) throws TransformerException {
		Source source = new DOMSource(dom);
		StringWriter stringWriter = new StringWriter();
		Result result = new StreamResult(stringWriter);
		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		/*
		 * We have to omit the ?xml declaration if we want to embed the
		 * document.
		 */
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(source, result);
		return stringWriter.getBuffer().toString();
	}
}
