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

package test.unit.be.fedict.eid.dss.document.zip;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import be.fedict.eid.dss.document.zip.ZIPSignatureOutputStream;

public class ZIPSignatureOutputStreamTest {

	private static final Log LOG = LogFactory
			.getLog(ZIPSignatureOutputStreamTest.class);

	@Test
	public void testZIPpackage() throws Exception {
		// setup
		File zipFile = File.createTempFile("test-", ".zip");
		zipFile.deleteOnExit();
		ZipOutputStream zipOutputStream = new ZipOutputStream(
				new FileOutputStream(zipFile));
		zipOutputStream.putNextEntry(new ZipEntry("a.txt"));
		IOUtils.write("hello world", zipOutputStream);
		zipOutputStream.putNextEntry(new ZipEntry("b.txt"));
		IOUtils.write("hello world 2", zipOutputStream);
		zipOutputStream.close();

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		ZIPSignatureOutputStream testedInstance = new ZIPSignatureOutputStream(
				zipFile, outputStream);

		// operate
		IOUtils.write("signature data", testedInstance);
		testedInstance.close();

		// verify
		ZipInputStream zipInputStream = new ZipInputStream(
				new ByteArrayInputStream(outputStream.toByteArray()));
		ZipEntry zipEntry;
		while (null != (zipEntry = zipInputStream.getNextEntry())) {
			LOG.debug("zip entry: " + zipEntry.getName());
		}
	}
}
