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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.applet.service.signer.odf.ODFUtil;

public class ZIPSignatureOutputStream extends ByteArrayOutputStream {

	private static final Log LOG = LogFactory
			.getLog(ZIPSignatureOutputStream.class);

	private final File originalZipFile;

	private final OutputStream targetOutputStream;

	public ZIPSignatureOutputStream(File originalZipFile,
			OutputStream targetOutputStream) {
		this.originalZipFile = originalZipFile;
		this.targetOutputStream = targetOutputStream;
	}

	@Override
	public void close() throws IOException {
		super.close();

		byte[] signatureData = toByteArray();

		/*
		 * Copy the original ZIP content.
		 */
		ZipOutputStream zipOutputStream = new ZipOutputStream(
				this.targetOutputStream);
		ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(
				this.originalZipFile));
		ZipEntry zipEntry;
		while (null != (zipEntry = zipInputStream.getNextEntry())) {
			if (!zipEntry.getName().equals(ODFUtil.SIGNATURE_FILE)) {
				ZipEntry newZipEntry = new ZipEntry(zipEntry.getName());
				zipOutputStream.putNextEntry(newZipEntry);
				LOG.debug("copying " + zipEntry.getName());
				IOUtils.copy(zipInputStream, zipOutputStream);
			}
		}
		zipInputStream.close();

		/*
		 * Add the XML signature file to the ZIP package.
		 */
		zipEntry = new ZipEntry(ODFUtil.SIGNATURE_FILE);
		LOG.debug("writing " + zipEntry.getName());
		zipOutputStream.putNextEntry(zipEntry);
		IOUtils.write(signatureData, zipOutputStream);
		zipOutputStream.close();
	}
}
