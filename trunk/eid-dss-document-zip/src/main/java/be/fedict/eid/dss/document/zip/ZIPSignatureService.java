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

import be.fedict.eid.applet.service.signer.*;
import be.fedict.eid.applet.service.signer.facets.*;
import be.fedict.eid.applet.service.signer.odf.ODFUtil;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.spi.AddressDTO;
import be.fedict.eid.applet.service.spi.DigestInfo;
import be.fedict.eid.applet.service.spi.IdentityDTO;
import be.fedict.eid.applet.service.spi.SignatureServiceEx;
import be.fedict.eid.dss.spi.utils.CloseActionOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.xml.security.utils.Constants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.crypto.URIDereferencer;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Signature service implementation for ZIP containers. We basically use the ODF
 * signature container format and sign everything within the ZIP.
 *
 * @author Frank Cornelis
 */
public class ZIPSignatureService extends AbstractXmlSignatureService implements
        SignatureServiceEx {

    private final TemporaryDataStorage temporaryDataStorage;

    private final OutputStream documentOutputStream;

    private final File tmpFile;

    public ZIPSignatureService(InputStream documentInputStream,
                               SignatureFacet signatureFacet, OutputStream documentOutputStream,
                               RevocationDataService revocationDataService,
                               TimeStampService timeStampService, String role,
                               IdentityDTO identity, byte[] photo, DigestAlgo signatureDigestAlgo)
            throws IOException {

        super(signatureDigestAlgo);
        this.temporaryDataStorage = new HttpSessionTemporaryDataStorage();
        this.documentOutputStream = documentOutputStream;

        this.tmpFile = File.createTempFile("eid-dss-", ".zip");
        FileOutputStream fileOutputStream;
        fileOutputStream = new FileOutputStream(this.tmpFile);
        IOUtils.copy(documentInputStream, fileOutputStream);

        addSignatureFacet(new ZIPSignatureFacet(this.tmpFile));
        XAdESSignatureFacet xadesSignatureFacet = new XAdESSignatureFacet(
                getSignatureDigestAlgorithm());
        xadesSignatureFacet.setRole(role);
        addSignatureFacet(xadesSignatureFacet);
        addSignatureFacet(new KeyInfoSignatureFacet(true, false, false));
        addSignatureFacet(new XAdESXLSignatureFacet(timeStampService,
                revocationDataService, getSignatureDigestAlgorithm()));
        addSignatureFacet(signatureFacet);

        if (null != identity) {
            IdentitySignatureFacet identitySignatureFacet = new IdentitySignatureFacet(
                    identity, photo, getSignatureDigestAlgorithm());
            addSignatureFacet(identitySignatureFacet);
        }
    }

    public String getFilesDigestAlgorithm() {
        return null;
    }

    @Override
    protected String getSignatureDescription() {
        return "ZIP container";
    }

    @Override
    protected OutputStream getSignedDocumentOutputStream() {
        return new ZIPSignatureOutputStream(this.tmpFile,
                new CloseActionOutputStream(this.documentOutputStream,
                        new CloseAction()));
    }

    private class CloseAction implements Runnable {
        public void run() {
            ZIPSignatureService.this.tmpFile.delete();
        }
    }

    @Override
    protected TemporaryDataStorage getTemporaryDataStorage() {
        return this.temporaryDataStorage;
    }

    @Override
    protected Document getEnvelopingDocument()
            throws ParserConfigurationException, IOException, SAXException {
        FileInputStream fileInputStream = new FileInputStream(this.tmpFile);
        ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
        ZipEntry zipEntry;
        while (null != (zipEntry = zipInputStream.getNextEntry())) {
            if (ODFUtil.isSignatureFile(zipEntry)) {
                Document documentSignaturesDocument = ODFUtil
                        .loadDocument(zipInputStream);
                return documentSignaturesDocument;
            }
        }
        Document document = ODFUtil.getNewDocument();
        Element rootElement = document.createElementNS(ODFUtil.SIGNATURE_NS,
                ODFUtil.SIGNATURE_ELEMENT);
        rootElement.setAttributeNS(Constants.NamespaceSpecNS, "xmlns",
                ODFUtil.SIGNATURE_NS);
        document.appendChild(rootElement);
        return document;
    }

    @Override
    protected URIDereferencer getURIDereferencer() {
        return new ZIPURIDereferencer(this.tmpFile);
    }

    public DigestInfo preSign(List<DigestInfo> digestInfos,
                              List<X509Certificate> signingCertificateChain,
                              IdentityDTO identity, AddressDTO address, byte[] photo)
            throws NoSuchAlgorithmException {
        return super.preSign(digestInfos, signingCertificateChain);
    }
}
