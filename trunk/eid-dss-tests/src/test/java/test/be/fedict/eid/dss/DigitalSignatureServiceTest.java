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
import be.fedict.eid.applet.sc.Pkcs11Eid;
import be.fedict.eid.dss.client.DigitalSignatureServiceClient;
import be.fedict.eid.dss.client.NotParseableXMLDocumentException;
import be.fedict.eid.dss.client.SignatureInfo;

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
		boolean result = client.verify("<test/>");

		// verify
		assertFalse(result);
	}

	@Test
	public void testVerifyNonXMLDocument() throws Exception {
		// setup
		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient();

		// operate & verify
		try {
			client.verify("foo-bar");
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
		boolean result = client.verify(signedDocument);

		// verify
		assertTrue(result);
	}

	@Test
	public void testVerifyXAdESXLSignedDocument() throws Exception {
		// setup
		InputStream signedDocumentInputStream = DigitalSignatureServiceTest.class
				.getResourceAsStream("/signed-document.xml");
		String signedDocument = IOUtils.toString(signedDocumentInputStream);

		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient();

		// operate
		String result = client.verifyWithSignerIdentity(signedDocument);

		// verify
		assertNotNull(result);
		LOG.debug("signed id: " + result);
		assertEquals("79102520991", result);
	}

	@Test
	public void testVerifyWithSigners() throws Exception {
		// setup
		InputStream signedDocumentInputStream = DigitalSignatureServiceTest.class
				.getResourceAsStream("/signed-document.xml");
		String signedDocument = IOUtils.toString(signedDocumentInputStream);

		DigitalSignatureServiceClient client = new DigitalSignatureServiceClient();

		// operate
		List<SignatureInfo> signers = client.verifyWithSigners(signedDocument);

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
	public void testSignedDocumentWithCertResult() throws Exception {
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
		String result = client.verifyWithSignerIdentity(signedDocument);

		// verify
		LOG.debug("result: " + result);
		assertNotNull(result);
	}

	private void signDocument(Document document) throws IOException,
			PKCS11Exception, InterruptedException, NoSuchFieldException,
			IllegalAccessException, InvocationTargetException,
			NoSuchMethodException, KeyStoreException, NoSuchAlgorithmException,
			CertificateException, UnrecoverableEntryException,
			InvalidAlgorithmParameterException, MarshalException,
			XMLSignatureException {
		Messages messages = new Messages(Locale.getDefault());
		Pkcs11Eid pkcs11Eid = new Pkcs11Eid(new TestView(), messages);
		if (false == pkcs11Eid.isEidPresent()) {
			LOG.debug("insert eID...");
			pkcs11Eid.waitForEidPresent();
		}
		PrivateKeyEntry privateKeyEntry = pkcs11Eid.getPrivateKeyEntry();

		XMLSignatureFactory signatureFactory = XMLSignatureFactory.getInstance(
				"DOM", new org.jcp.xml.dsig.internal.dom.XMLDSigRI());
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

		pkcs11Eid.close();
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
