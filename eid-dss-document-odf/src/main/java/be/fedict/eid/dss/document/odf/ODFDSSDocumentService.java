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

package be.fedict.eid.dss.document.odf;

import be.fedict.eid.applet.service.signer.DigestAlgo;
import be.fedict.eid.applet.service.signer.KeyInfoKeySelector;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.odf.ODFURIDereferencer;
import be.fedict.eid.applet.service.signer.odf.ODFUtil;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.applet.service.spi.IdentityDTO;
import be.fedict.eid.applet.service.spi.SignatureServiceEx;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.DSSDocumentService;
import be.fedict.eid.dss.spi.DocumentVisualization;
import be.fedict.eid.dss.spi.SignatureInfo;
import be.fedict.eid.dss.spi.utils.XAdESValidation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Document Service implementation of OpenOffice formats.
 * 
 * @author Frank Cornelis
 */
public class ODFDSSDocumentService implements DSSDocumentService {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(ODFDSSDocumentService.class);

	private DSSDocumentContext documentContext;

	public void init(DSSDocumentContext context, String contentType)
			throws Exception {

		LOG.debug("init");
		this.documentContext = context;
	}

	public void checkIncomingDocument(byte[] document) throws Exception {

		LOG.debug("checkIncomingDocument");
	}

	public DocumentVisualization visualizeDocument(byte[] document,
			String language) throws Exception {

		LOG.debug("visualizeDocument");
		return null;
	}

	public SignatureServiceEx getSignatureService(
			InputStream documentInputStream, TimeStampService timeStampService,
			TimeStampServiceValidator timeStampServiceValidator,
			RevocationDataService revocationDataService,
			SignatureFacet signatureFacet, OutputStream documentOutputStream,
			String role, IdentityDTO identity, byte[] photo,
			DigestAlgo signatureDigestAlgo) throws Exception {

		LOG.debug("getSignatureService");
		return new ODFSignatureService(timeStampServiceValidator,
				revocationDataService, signatureFacet, documentInputStream,
				documentOutputStream, timeStampService, role, identity, photo,
				signatureDigestAlgo);
	}

	public List<SignatureInfo> verifySignatures(byte[] document)
			throws Exception {

		List<SignatureInfo> signatureInfos = new LinkedList<SignatureInfo>();
		ZipInputStream odfZipInputStream = new ZipInputStream(
				new ByteArrayInputStream(document));
		ZipEntry zipEntry;
		while (null != (zipEntry = odfZipInputStream.getNextEntry())) {
			if (ODFUtil.isSignatureFile(zipEntry)) {
				Document documentSignatures = ODFUtil
						.loadDocument(odfZipInputStream);
				NodeList signatureNodeList = documentSignatures
						.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");

				XAdESValidation xadesValidation = new XAdESValidation(
						this.documentContext);

				for (int idx = 0; idx < signatureNodeList.getLength(); idx++) {
					Element signatureElement = (Element) signatureNodeList
							.item(idx);
					KeyInfoKeySelector keySelector = new KeyInfoKeySelector();
					DOMValidateContext domValidateContext = new DOMValidateContext(
							keySelector, signatureElement);
					ODFURIDereferencer dereferencer = new ODFURIDereferencer(
							document);
					domValidateContext.setURIDereferencer(dereferencer);

					XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
							.getInstance();
					XMLSignature xmlSignature = xmlSignatureFactory
							.unmarshalXMLSignature(domValidateContext);
					boolean valid = xmlSignature.validate(domValidateContext);
					if (!valid) {
						LOG.debug("invalid signature");
						continue;
					}
					X509Certificate signingCertificate = keySelector
							.getCertificate();
					SignatureInfo signatureInfo = xadesValidation.validate(
							documentSignatures, xmlSignature, signatureElement,
							signingCertificate);
					signatureInfos.add(signatureInfo);
				}
				return signatureInfos;
			}
		}
		return signatureInfos;
	}
}
