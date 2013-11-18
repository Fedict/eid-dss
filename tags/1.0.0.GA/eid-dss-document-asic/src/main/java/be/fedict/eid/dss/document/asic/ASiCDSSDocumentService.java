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

package be.fedict.eid.dss.document.asic;

import be.fedict.eid.applet.service.signer.DigestAlgo;
import be.fedict.eid.applet.service.signer.KeyInfoKeySelector;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.asic.ASiCURIDereferencer;
import be.fedict.eid.applet.service.signer.asic.ASiCUtil;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.odf.ODFUtil;
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

import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.SignedInfo;
import javax.xml.crypto.dsig.XMLSignature;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Associated Signature Container document service implementation.
 * <p/>
 * Specification: ETSI TS 102 918 v1.1.1 (2011-04)
 * 
 * @author Frank Cornelis
 * 
 */
public class ASiCDSSDocumentService implements DSSDocumentService {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(ASiCDSSDocumentService.class);

	private DSSDocumentContext documentContext;

	@Override
	public void init(DSSDocumentContext context, String contentType)
			throws Exception {
		this.documentContext = context;
	}

	@Override
	public void checkIncomingDocument(byte[] document) throws Exception {
	}

    public DocumentVisualization findDocument(byte[] parentDocument, String resourceId)
            throws Exception {

        return null;
    }

    @Override
	public DocumentVisualization visualizeDocument(byte[] document,
			String language, List<MimeType> mimeTypes,
            String documentViewerServlet) throws Exception {
		ZipInputStream zipInputStream = new ZipInputStream(
				new ByteArrayInputStream(document));
		ZipEntry zipEntry;
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("<html>");
		stringBuffer.append("<head>");
		stringBuffer
				.append("<meta http-equiv=\"Content-Type\" content=\"text/html;charset=utf-8\">");
		stringBuffer.append("<title>Associated Signature Container</title>");
		stringBuffer.append("</head>");
		stringBuffer.append("<body>");
		stringBuffer.append("<h1>Associated Signature Container</h1>");
		while (null != (zipEntry = zipInputStream.getNextEntry())) {
			if (ASiCUtil.isSignatureZipEntry(zipEntry)) {
				continue;
			}
			String zipEntryName = zipEntry.getName();
			if ("META-INF/container.xml".equals(zipEntryName)) {
				continue;
			}
			if ("META-INF/manifest.xml".equals(zipEntryName)) {
				continue;
			}
			if ("META-INF/metadata.xml".equals(zipEntryName)) {
				continue;
			}
			if ("mimetype".equals(zipEntryName)) {
				continue;
			}
			if (zipEntryName.startsWith("META-INF/")) {
				if (zipEntryName.endsWith(".xml")) {
					if (zipEntryName.indexOf("signatures") != -1) {
						continue;
					}
				}
			}
			stringBuffer.append("<p>" + zipEntryName + "</p>");
		}
		stringBuffer.append("</body></html>");

		return new DocumentVisualization("text/html;charset=utf-8",
				stringBuffer.toString().getBytes());
	}

	@Override
	public SignatureServiceEx getSignatureService(
			InputStream documentInputStream, TimeStampService timeStampService,
			TimeStampServiceValidator timeStampServiceValidator,
			RevocationDataService revocationDataService,
			SignatureFacet signatureFacet, OutputStream documentOutputStream,
			String role, IdentityDTO identity, byte[] photo,
			DigestAlgo signatureDigestAlgo) throws Exception {
		return new ASiCSignatureService(documentInputStream,
				signatureDigestAlgo, revocationDataService, timeStampService,
				role, identity, photo, documentOutputStream, signatureFacet);
	}

	@Override
	public List<SignatureInfo> verifySignatures(byte[] document,
			byte[] originalDocument) throws Exception {
		if (null != originalDocument) {
			throw new IllegalArgumentException(
					"cannot perform original document verifications");
		}
		ZipInputStream zipInputStream = new ZipInputStream(
				new ByteArrayInputStream(document));
		ZipEntry zipEntry;
		while (null != (zipEntry = zipInputStream.getNextEntry())) {
			if (ASiCUtil.isSignatureZipEntry(zipEntry)) {
				break;
			}
		}
		List<SignatureInfo> signatureInfos = new LinkedList<SignatureInfo>();
		if (null == zipEntry) {
			return signatureInfos;
		}
		XAdESValidation xadesValidation = new XAdESValidation(
				this.documentContext);
		Document documentSignaturesDocument = ODFUtil
				.loadDocument(zipInputStream);
		NodeList signatureNodeList = documentSignaturesDocument
				.getElementsByTagNameNS(XMLSignature.XMLNS, "Signature");
		for (int idx = 0; idx < signatureNodeList.getLength(); idx++) {
			Element signatureElement = (Element) signatureNodeList.item(idx);
			xadesValidation.prepareDocument(signatureElement);
			KeyInfoKeySelector keySelector = new KeyInfoKeySelector();
			DOMValidateContext domValidateContext = new DOMValidateContext(
					keySelector, signatureElement);
			ASiCURIDereferencer dereferencer = new ASiCURIDereferencer(document);
			domValidateContext.setURIDereferencer(dereferencer);

			XMLSignatureFactory xmlSignatureFactory = XMLSignatureFactory
					.getInstance();
			XMLSignature xmlSignature = xmlSignatureFactory
					.unmarshalXMLSignature(domValidateContext);
			boolean valid = xmlSignature.validate(domValidateContext);
			if (!valid) {
				continue;
			}

			// check whether all files have been signed properly
			SignedInfo signedInfo = xmlSignature.getSignedInfo();
			@SuppressWarnings("unchecked")
			List<Reference> references = signedInfo.getReferences();
			Set<String> referenceUris = new HashSet<String>();
			for (Reference reference : references) {
				String referenceUri = reference.getURI();
				referenceUris.add(URLDecoder.decode(referenceUri, "UTF-8"));
			}
			zipInputStream = new ZipInputStream(new ByteArrayInputStream(
					document));
			while (null != (zipEntry = zipInputStream.getNextEntry())) {
				if (ASiCUtil.isSignatureZipEntry(zipEntry)) {
					continue;
				}
				if (false == referenceUris.contains(zipEntry.getName())) {
					LOG.warn("no ds:Reference for ASiC entry: "
							+ zipEntry.getName());
					return signatureInfos;
				}
			}

			X509Certificate signer = keySelector.getCertificate();
			SignatureInfo signatureInfo = xadesValidation.validate(
					documentSignaturesDocument, xmlSignature, signatureElement,
					signer);
			signatureInfos.add(signatureInfo);
		}
		return signatureInfos;
	}
}
