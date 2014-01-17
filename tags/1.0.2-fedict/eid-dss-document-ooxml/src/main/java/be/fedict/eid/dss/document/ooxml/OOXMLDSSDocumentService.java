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

package be.fedict.eid.dss.document.ooxml;

import be.fedict.eid.applet.service.signer.DigestAlgo;
import be.fedict.eid.applet.service.signer.KeyInfoKeySelector;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.ooxml.OOXMLProvider;
import be.fedict.eid.applet.service.signer.ooxml.OOXMLSignatureVerifier;
import be.fedict.eid.applet.service.signer.ooxml.OOXMLURIDereferencer;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.applet.service.spi.IdentityDTO;
import be.fedict.eid.applet.service.spi.SignatureServiceEx;
import be.fedict.eid.dss.spi.*;
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

public class OOXMLDSSDocumentService implements DSSDocumentService {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(OOXMLDSSDocumentService.class);

	private DSSDocumentContext documentContext;

	public void init(DSSDocumentContext context, String contentType)
			throws Exception {

		LOG.debug("init");
		this.documentContext = context;
		/*
		 * Next will probably break re-deployments.
		 */
		OOXMLProvider.install();
	}

	public void checkIncomingDocument(byte[] document) throws Exception {

		LOG.debug("checkIncomingDocument");
	}

	public DocumentVisualization findDocument(byte[] parentDocument,
			String resourceId) throws Exception {

		return null;
	}

	public DocumentVisualization visualizeDocument(byte[] document,
			String language, List<MimeType> mimeTypes,
			String documentViewerServlet) throws Exception {

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

		return new OOXMLSignatureService(documentInputStream,
				documentOutputStream, signatureFacet, role, identity, photo,
				revocationDataService, timeStampService, signatureDigestAlgo,
				this.documentContext);
	}

	@Override
	public List<SignatureInfo> verifySignatures(byte[] document,
			byte[] originalDocument) throws Exception {
		if (null != originalDocument) {
			throw new IllegalArgumentException(
					"cannot perform original document verifications");
		}

		OOXMLSignatureVerifier ooxmlSignatureVerifier = new OOXMLSignatureVerifier();
		List<String> signatureResourceNames = ooxmlSignatureVerifier
				.getSignatureResourceNames(document);
		List<SignatureInfo> signatureInfos = new LinkedList<SignatureInfo>();
		XAdESValidation xadesValidation = new XAdESValidation(
				this.documentContext);
		for (String signatureResourceName : signatureResourceNames) {
            LOG.debug("signatureResourceName: " + signatureResourceName);
			Document signatureDocument = ooxmlSignatureVerifier
					.getSignatureDocument(new ByteArrayInputStream(document),
							signatureResourceName);
			if (null == signatureDocument) {
				continue;
			}
			NodeList signatureNodeList = signatureDocument
					.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
			if (0 == signatureNodeList.getLength()) {
				continue;
			}
			Element signatureElement = (Element) signatureNodeList.item(0);
			xadesValidation.prepareDocument(signatureElement);
			KeyInfoKeySelector keySelector = new KeyInfoKeySelector();
			DOMValidateContext domValidateContext = new DOMValidateContext(
					keySelector, signatureElement);
			domValidateContext.setProperty(
					"org.jcp.xml.dsig.validateManifests", Boolean.TRUE);
			OOXMLURIDereferencer dereferencer = new OOXMLURIDereferencer(
					document);
			domValidateContext.setURIDereferencer(dereferencer);

			XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
					.getInstance();
			XMLSignature xmlSignature = xmlSignatureFactory
					.unmarshalXMLSignature(domValidateContext);
            LOG.debug("validating signature: " + xmlSignature.getId());
			boolean valid = xmlSignature.validate(domValidateContext);
            LOG.debug("signature valid: " + valid);
			if (!valid) {
				LOG.error("signature invalid");
				continue;
			}

			// check OOXML's XML DSig/XAdES requirements
			if (!ooxmlSignatureVerifier.isValidOOXMLSignature(xmlSignature,
					document)) {
				LOG.error("Invalid OOXML Signature");
				continue;
			}

			X509Certificate signingCertificate = keySelector.getCertificate();
			SignatureInfo signatureInfo = xadesValidation.validate(
					signatureDocument, xmlSignature, signatureElement,
					signingCertificate);
			signatureInfos.add(signatureInfo);
		}
		return signatureInfos;
	}
}
