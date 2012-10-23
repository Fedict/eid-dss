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

package test.unit.be.fedict.eid.dss.spi.utils;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.Cipher;
import javax.xml.crypto.KeySelector;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.utils.Constants;
import org.apache.xpath.XPathAPI;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import be.fedict.eid.applet.service.signer.AbstractXmlSignatureService;
import be.fedict.eid.applet.service.signer.DigestAlgo;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.TemporaryDataStorage;
import be.fedict.eid.applet.service.signer.facets.ExplicitSignaturePolicyService;
import be.fedict.eid.applet.service.signer.facets.RevocationData;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.facets.SignaturePolicyService;
import be.fedict.eid.applet.service.signer.facets.XAdESSignatureFacet;
import be.fedict.eid.applet.service.signer.facets.XAdESXLSignatureFacet;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.utils.XAdESUtils;
import be.fedict.eid.dss.spi.utils.XAdESValidation;
import be.fedict.eid.dss.spi.utils.exception.XAdESValidationException;

public class XAdESValidationTest {

	private static final Log LOG = LogFactory.getLog(XAdESValidationTest.class);

	private KeyPair keyPair;
	private X509Certificate certificate;

	@BeforeClass
	public static void beforeClass() {
		if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
			Security.addProvider(new BouncyCastleProvider());
		}
	}

	@Before
	public void setup() throws Exception {

		this.keyPair = PkiTestUtils.generateKeyPair();
		DateTime notBefore = new DateTime();
		DateTime notAfter = notBefore.plusYears(1);
		this.certificate = PkiTestUtils.generateCertificate(this.keyPair
				.getPublic(), "CN=Test", notBefore, notAfter, null,
				this.keyPair.getPrivate(), true, 0, null, null, new KeyUsage(
						KeyUsage.nonRepudiation), false);

	}

	@Test
	@SuppressWarnings("unchecked")
	public void testXAdESValidationSuccess() throws Exception {

		// Setup: signed document
		Document signedDocument = getSignedDocument(false);
		Node signatureNode = getSignatureNode(signedDocument);
		XMLSignature xmlSignature = getXmlSignature(signatureNode);

		// Setup: XAdESValidation
		DSSDocumentContext mockDSSDocumentContext = createMock(DSSDocumentContext.class);

		// expectations
		mockDSSDocumentContext.validate((TimeStampToken) EasyMock.anyObject());
		mockDSSDocumentContext.validate((TimeStampToken) EasyMock.anyObject());
		mockDSSDocumentContext.validate(
				(List<X509Certificate>) EasyMock.anyObject(),
				(Date) EasyMock.anyObject(),
				(List<OCSPResp>) EasyMock.anyObject(),
				(List<X509CRL>) EasyMock.anyObject());
		// XXX: slow running unit tests might fail here
		expect(mockDSSDocumentContext.getTimestampMaxOffset()).andReturn(
				20 * 1000L);
		expect(mockDSSDocumentContext.getMaxGracePeriod()).andReturn(
				1000L * 60 * 60 * 24 * 7);

		// prepare
		replay(mockDSSDocumentContext);

		// Operate: XAdESValidation
		new XAdESValidation(mockDSSDocumentContext).validate(signedDocument,
				xmlSignature, (Element) signatureNode, this.certificate);

		// verify
		verify(mockDSSDocumentContext);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testXAdESValidationWithTimeStampRevocationData()
			throws Exception {

		// Setup: signed document
		Document signedDocument = getSignedDocument(true);
		Node signatureNode = getSignatureNode(signedDocument);
		XMLSignature xmlSignature = getXmlSignature(signatureNode);

		// Setup: XAdESValidation
		DSSDocumentContext mockDSSDocumentContext = createMock(DSSDocumentContext.class);

		// expectations
		mockDSSDocumentContext.validate((TimeStampToken) EasyMock.anyObject(),
				(List<OCSPResp>) EasyMock.anyObject(),
				(List<X509CRL>) EasyMock.anyObject());
		mockDSSDocumentContext.validate((TimeStampToken) EasyMock.anyObject(),
				(List<OCSPResp>) EasyMock.anyObject(),
				(List<X509CRL>) EasyMock.anyObject());
		mockDSSDocumentContext.validate(
				(List<X509Certificate>) EasyMock.anyObject(),
				(Date) EasyMock.anyObject(),
				(List<OCSPResp>) EasyMock.anyObject(),
				(List<X509CRL>) EasyMock.anyObject());
		expect(mockDSSDocumentContext.getTimestampMaxOffset()).andReturn(
				10 * 1000L);
		expect(mockDSSDocumentContext.getMaxGracePeriod()).andReturn(
				1000L * 60 * 60 * 24 * 7);

		// prepare
		replay(mockDSSDocumentContext);

		// Operate: XAdESValidation
		new XAdESValidation(mockDSSDocumentContext).validate(signedDocument,
				xmlSignature, (Element) signatureNode, this.certificate);

		// verify
		verify(mockDSSDocumentContext);
	}

	@Test
	public void testXAdESValidationWrongSigningCertificate() throws Exception {

		// Setup: signed document
		Document signedDocument = getSignedDocument(false);
		Node signatureNode = getSignatureNode(signedDocument);
		XMLSignature xmlSignature = getXmlSignature(signatureNode);

		// Setup: wrong certificate
		DateTime notBefore = new DateTime();
		DateTime notAfter = notBefore.plusYears(1);
		X509Certificate wrongCertificate = PkiTestUtils.generateCertificate(
				this.keyPair.getPublic(), "CN=Test Wrong", notBefore, notAfter,
				null, this.keyPair.getPrivate(), true, 0, null, null,
				new KeyUsage(KeyUsage.nonRepudiation), false);

		// Setup: XAdESValidation
		DSSDocumentContext mockDSSDocumentContext = createMock(DSSDocumentContext.class);

		// prepare
		replay(mockDSSDocumentContext);

		// Operate: XAdESValidation
		try {
			new XAdESValidation(mockDSSDocumentContext).validate(
					signedDocument, xmlSignature, (Element) signatureNode,
					wrongCertificate);
			fail();
		} catch (XAdESValidationException e) {
			// expected
			LOG.error(e);
		}

		// verify
		verify(mockDSSDocumentContext);
	}

	@Test
	public void testJodaTime() throws Exception {
		DateTime t1 = new DateTime();
		DateTime t2 = t1.plusSeconds(10);

		assertTrue(t1.isBefore(t2));
		Duration dt = new Duration(t1, t2);
		LOG.debug("dt: " + dt);
		assertFalse(dt.isShorterThan(new Duration(10 * 1000)));
		assertTrue(dt.isShorterThan(new Duration(10 * 1000 + 1)));
	}

	private Document getTestDocument() throws Exception {

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
		dataElement.setAttributeNS(null, "Id", "id-1234");
		dataElement.setTextContent("data to be signed");
		rootElement.appendChild(dataElement);
		return document;
	}

	private Document getSignedDocument(boolean tsaRevocationData)
			throws Exception {

		// setup
		Document testDocument = getTestDocument();

		// setup: xades signature facets
		SignaturePolicyService signaturePolicyService = new ExplicitSignaturePolicyService(
				"urn:test", "hello world".getBytes(), "description",
				"http://here.com");
		XAdESSignatureFacet xadesSignatureFacet = new XAdESSignatureFacet(
				signaturePolicyService);
		TimeStampService testTimeStampService = new TestTimeStampService(
				tsaRevocationData);
		RevocationDataService mockRevocationDataService = EasyMock
				.createMock(RevocationDataService.class);
		XAdESXLSignatureFacet xadesXLSignatureFacet = new XAdESXLSignatureFacet(
				testTimeStampService, mockRevocationDataService);

		// setup: signature test service
		XmlSignatureTestService testedInstance = new XmlSignatureTestService(
				xadesSignatureFacet, xadesXLSignatureFacet);
		testedInstance.setEnvelopingDocument(testDocument);
		testedInstance.setSignatureDescription("test-signature-description");

		// setup: revocation data, ...
		List<X509Certificate> certificateChain = new LinkedList<X509Certificate>();
		/*
		 * We need at least 2 certificates for the XAdES-C complete certificate
		 * refs construction.
		 */
		certificateChain.add(certificate);
		certificateChain.add(certificate);

		RevocationData revocationData = new RevocationData();
		final X509CRL crl = PkiTestUtils.generateCrl(certificate,
				keyPair.getPrivate());
		revocationData.addCRL(crl);
		OCSPResp ocspResp = PkiTestUtils.createOcspResp(certificate, false,
				certificate, certificate, keyPair.getPrivate(), "SHA1withRSA");
		revocationData.addOCSP(ocspResp.getEncoded());

		// expectations
		EasyMock.expect(
				mockRevocationDataService.getRevocationData(EasyMock
						.eq(certificateChain))).andStubReturn(revocationData);

		// prepare
		EasyMock.replay(mockRevocationDataService);

		// operate
		DigestInfo digestInfo = testedInstance.preSign(null, certificateChain);

		// verify
		assertNotNull(digestInfo);
		LOG.debug("digest info description: " + digestInfo.description);
		assertEquals("test-signature-description", digestInfo.description);
		assertNotNull(digestInfo.digestValue);
		LOG.debug("digest algo: " + digestInfo.digestAlgo);
		assertEquals("SHA-1", digestInfo.digestAlgo);

		TemporaryTestDataStorage temporaryDataStorage = (TemporaryTestDataStorage) testedInstance
				.getTemporaryDataStorage();
		assertNotNull(temporaryDataStorage);
		InputStream tempInputStream = temporaryDataStorage.getTempInputStream();
		assertNotNull(tempInputStream);
		Document tmpDocument = PkiTestUtils.loadDocument(tempInputStream);

		LOG.debug("tmp document: " + PkiTestUtils.toString(tmpDocument));
		Element nsElement = tmpDocument.createElement("ns");
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:ds",
				Constants.SignatureSpecNS);
		nsElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns:xades",
				XAdESUtils.XADES_132_NS_URI);
		Node digestValueNode = XPathAPI.selectSingleNode(tmpDocument,
				"//ds:DigestValue", nsElement);
		assertNotNull(digestValueNode);
		String digestValueTextContent = digestValueNode.getTextContent();
		LOG.debug("digest value text content: " + digestValueTextContent);
		assertFalse(digestValueTextContent.isEmpty());

		/*
		 * Sign the received XML signature digest value.
		 */
		Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
		cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
		byte[] digestInfoValue = ArrayUtils.addAll(
				PkiTestUtils.SHA1_DIGEST_INFO_PREFIX, digestInfo.digestValue);
		byte[] signatureValue = cipher.doFinal(digestInfoValue);

		/*
		 * Operate: postSign
		 */
		testedInstance.postSign(signatureValue, certificateChain);

		byte[] signedDocumentData = testedInstance.getSignedDocumentData();
		assertNotNull(signedDocumentData);

		Document signedDocument = PkiTestUtils
				.loadDocument(new ByteArrayInputStream(signedDocumentData));
		LOG.debug("signed document: " + PkiTestUtils.toString(signedDocument));
		return signedDocument;
	}

	private XMLSignature getXmlSignature(Node signatureNode) throws Exception {

		DOMValidateContext domValidateContext = new DOMValidateContext(
				KeySelector.singletonKeySelector(keyPair.getPublic()),
				signatureNode);
		XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
				.getInstance();
		XMLSignature xmlSignature = xmlSignatureFactory
				.unmarshalXMLSignature(domValidateContext);
		boolean validity = xmlSignature.validate(domValidateContext);
		assertTrue(validity);
		return xmlSignature;
	}

	private Node getSignatureNode(Document signedDocument) {

		NodeList signatureNodeList = signedDocument.getElementsByTagNameNS(
				XMLSignature.XMLNS, "Signature");
		assertEquals(1, signatureNodeList.getLength());
		return signatureNodeList.item(0);
	}

	private static class XmlSignatureTestService extends
			AbstractXmlSignatureService {

		private Document envelopingDocument;

		private TemporaryTestDataStorage temporaryDataStorage;

		private String signatureDescription;

		private ByteArrayOutputStream signedDocumentOutputStream;

		public XmlSignatureTestService(SignatureFacet... signatureFacets) {
			super(DigestAlgo.SHA1);
			this.temporaryDataStorage = new TemporaryTestDataStorage();
			this.signedDocumentOutputStream = new ByteArrayOutputStream();
			for (SignatureFacet signatureFacet : signatureFacets) {
				addSignatureFacet(signatureFacet);
			}
			setSignatureNamespacePrefix("ds");
		}

		public byte[] getSignedDocumentData() {
			return this.signedDocumentOutputStream.toByteArray();
		}

		public void setEnvelopingDocument(Document envelopingDocument) {
			this.envelopingDocument = envelopingDocument;
		}

		@Override
		protected Document getEnvelopingDocument() {
			return this.envelopingDocument;
		}

		@Override
		protected String getSignatureDescription() {
			return this.signatureDescription;
		}

		public void setSignatureDescription(String signatureDescription) {
			this.signatureDescription = signatureDescription;
		}

		@Override
		protected OutputStream getSignedDocumentOutputStream() {
			return this.signedDocumentOutputStream;
		}

		@Override
		protected TemporaryDataStorage getTemporaryDataStorage() {
			return this.temporaryDataStorage;
		}

		public String getFilesDigestAlgorithm() {
			return null;
		}
	}

	private class TestTimeStampService implements TimeStampService {

		private KeyPair tsaKeyPair;
		private List<X509Certificate> tsaCertificateChain;
		private X509CRL tsaCrl;

		public TestTimeStampService(boolean tsaRevocationData) throws Exception {

			this.tsaKeyPair = PkiTestUtils.generateKeyPair();
			DateTime notBefore = new DateTime();
			DateTime notAfter = notBefore.plusYears(1);
			X509Certificate tsaCertificate = PkiTestUtils.generateCertificate(
					this.tsaKeyPair.getPublic(), "CN=Test TSA", notBefore,
					notAfter, null, this.tsaKeyPair.getPrivate(), true, 0,
					null, null, null, true);
			this.tsaCertificateChain = new LinkedList<X509Certificate>();
			this.tsaCertificateChain.add(tsaCertificate);

			if (tsaRevocationData) {
				this.tsaCrl = PkiTestUtils.generateCrl(tsaCertificate,
						this.tsaKeyPair.getPrivate());
			}
		}

		@Override
		public byte[] timeStamp(byte[] bytes, RevocationData revocationData)
				throws Exception {

			TimeStampToken timeStampToken = PkiTestUtils.createTimeStampToken(
					bytes, this.tsaKeyPair.getPrivate(),
					this.tsaCertificateChain);

			if (null != this.tsaCrl) {
				revocationData.addCRL(this.tsaCrl);
			}

			return timeStampToken.getEncoded();
		}
	}

}
