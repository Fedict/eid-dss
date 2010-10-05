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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.apache.commons.io.IOUtils;

import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.TemporaryDataStorage;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.facets.XAdESSignatureFacet;
import be.fedict.eid.applet.service.signer.facets.XAdESXLSignatureFacet;
import be.fedict.eid.applet.service.signer.odf.AbstractODFSignatureService;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.dss.spi.utils.CloseActionOutputStream;

public class ODFSignatureService extends AbstractODFSignatureService {

	private final TemporaryDataStorage temporaryDataStorage;

	private final OutputStream documentOutputStream;

	private final File tmpFile;

	public ODFSignatureService(
			TimeStampServiceValidator timeStampServiceValidator,
			RevocationDataService revocationDataService,
			SignatureFacet signatureFacet, InputStream documentInputStream,
			OutputStream documentOutputStream,
			TimeStampService timeStampService, String role) throws Exception {
		this.temporaryDataStorage = new HttpSessionTemporaryDataStorage();
		this.documentOutputStream = documentOutputStream;
		this.tmpFile = File.createTempFile("eid-dss-", ".odf");
		FileOutputStream fileOutputStream;
		fileOutputStream = new FileOutputStream(this.tmpFile);
		IOUtils.copy(documentInputStream, fileOutputStream);
		addSignatureFacet(new XAdESXLSignatureFacet(timeStampService,
				revocationDataService, "SHA-512"));
		addSignatureFacet(signatureFacet);

		XAdESSignatureFacet xadesSignatureFacet = super
				.getXAdESSignatureFacet();
		xadesSignatureFacet.setRole(role);
	}

	@Override
	protected URL getOpenDocumentURL() {
		try {
			return this.tmpFile.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException("URL error: " + e.getMessage(), e);
		}
	}

	@Override
	protected OutputStream getSignedOpenDocumentOutputStream() {
		return new CloseActionOutputStream(this.documentOutputStream,
				new CloseAction());
	}

	private class CloseAction implements Runnable {
		public void run() {
			ODFSignatureService.this.tmpFile.delete();
		}
	}

	@Override
	protected TemporaryDataStorage getTemporaryDataStorage() {
		return this.temporaryDataStorage;
	}
}
