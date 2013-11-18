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

package be.fedict.eid.dss.document.zip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URLEncoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignatureFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import be.fedict.eid.applet.service.signer.DigestAlgo;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.odf.ODFUtil;

public class ZIPSignatureFacet implements SignatureFacet {

	private final File tmpZipFile;
	private final DigestAlgo digestAlgo;

	public ZIPSignatureFacet(File tmpZipFile, DigestAlgo digestAlgo) {
		this.tmpZipFile = tmpZipFile;
		this.digestAlgo = digestAlgo;
	}

	public void postSign(Element signatureElement,
			List<X509Certificate> signingCertificateChain) {
		// empty
	}

	public void preSign(XMLSignatureFactory signatureFactory,
			Document document, String signatureId,
			List<X509Certificate> signingCertificateChain,
			List<Reference> references, List<XMLObject> objects)
			throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		FileInputStream fileInputStream;
		try {
			fileInputStream = new FileInputStream(this.tmpZipFile);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("tmp file not found: " + e.getMessage(),
					e);
		}

		DigestMethod digestMethod = signatureFactory.newDigestMethod(
				digestAlgo.getXmlAlgoId(), null);

		ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
		ZipEntry zipEntry;
		try {
			while (null != (zipEntry = zipInputStream.getNextEntry())) {
				if (ODFUtil.isSignatureFile(zipEntry)) {
					continue;
				}
				String uri = URLEncoder.encode(zipEntry.getName(), "UTF-8");
				Reference reference = signatureFactory.newReference(uri,
						digestMethod);
				references.add(reference);
			}
		} catch (IOException e) {
			throw new RuntimeException("I/O error: " + e.getMessage(), e);
		}
	}
}
