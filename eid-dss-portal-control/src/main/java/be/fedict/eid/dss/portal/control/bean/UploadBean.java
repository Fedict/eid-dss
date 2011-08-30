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

package be.fedict.eid.dss.portal.control.bean;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.log.Log;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

import be.fedict.eid.dss.portal.control.Upload;

@Stateful
@Name("dssUpload")
@LocalBinding(jndiBinding = "fedict/eid/dss/portal/UploadBean")
public class UploadBean implements Upload {

	@Logger
	private Log log;

	@In(value = "filename", scope = ScopeType.SESSION, required = false)
	@Out(value = "filename", scope = ScopeType.SESSION, required = false)
	private String filename;

	@In(value = "ContentType", scope = ScopeType.SESSION, required = false)
	@Out(value = "ContentType", scope = ScopeType.SESSION, required = false)
	private String contentType;

	@In(value = "document", scope = ScopeType.SESSION, required = false)
	@Out(value = "document", scope = ScopeType.SESSION, required = false)
	private byte[] document;

	@Override
	public String done() {
		this.log.debug("done");
		return "done";
	}

	private static final Map<String, String> supportedFileExtensions;

	static {
		supportedFileExtensions = new HashMap<String, String>();

		// XML document container.
		supportedFileExtensions.put("xml", "text/xml");

		// Open Document Format
		supportedFileExtensions.put("odt",
				"application/vnd.oasis.opendocument.text");
		supportedFileExtensions.put("ods",
				"application/vnd.oasis.opendocument.spreadsheet");
		supportedFileExtensions.put("odp",
				"application/vnd.oasis.opendocument.presentation");
		supportedFileExtensions.put("odg",
				"application/vnd.oasis.opendocument.graphics");
		supportedFileExtensions.put("odc",
				"application/vnd.oasis.opendocument.chart");
		supportedFileExtensions.put("odf",
				"application/vnd.oasis.opendocument.formula");
		supportedFileExtensions.put("odi",
				"application/vnd.oasis.opendocument.image");

		// Office OpenXML.
		supportedFileExtensions
				.put("docx",
						"application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		supportedFileExtensions
				.put("xlsx",
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		supportedFileExtensions
				.put("pptx",
						"application/vnd.openxmlformats-officedocument.presentationml.presentation");
		supportedFileExtensions
				.put("ppsx",
						"application/vnd.openxmlformats-officedocument.presentationml.slideshow");

		// ZIP containers.
		supportedFileExtensions.put("zip", "application/zip");

		// Associated Signature Container (ETSI TS 102 918 v1.1.1)
		supportedFileExtensions.put("asics", "application/vnd.etsi.asic-s+zip");
		supportedFileExtensions.put("scs", "application/vnd.etsi.asic-s+zip");

		supportedFileExtensions.put("asice", "application/vnd.etsi.asic-e+zip");
		supportedFileExtensions.put("sce", "application/vnd.etsi.asic-e+zip");
	}

	@Override
	public void listener(UploadEvent event) throws Exception {
		this.log.debug("listener");
		UploadItem item = event.getUploadItem();
		this.log.debug("filename: #0", item.getFileName());
		this.filename = item.getFileName();
		this.log.debug("content type: #0", item.getContentType());
		String extension = FilenameUtils.getExtension(this.filename)
				.toLowerCase();
		this.contentType = supportedFileExtensions.get(extension);
		if (null == this.contentType) {
			/*
			 * Unsupported content-type is converted to a ZIP container.
			 */
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
			ZipEntry zipEntry = new ZipEntry(this.filename);
			zipOutputStream.putNextEntry(zipEntry);
			IOUtils.write(item.getData(), zipOutputStream);
			zipOutputStream.close();
			this.filename = FilenameUtils.getBaseName(this.filename) + ".zip";
			this.document = outputStream.toByteArray();
			this.contentType = "application/zip";
			return;
		}
		this.log.debug("file size: #0", item.getFileSize());
		this.log.debug("data bytes available: #0", (null != item.getData()));
		if (null != item.getData()) {
			this.document = item.getData();
			return;
		}
		File file = item.getFile();
		if (null != file) {
			this.log.debug("tmp file: #0", file.getAbsolutePath());
			this.document = FileUtils.readFileToByteArray(file);
		}
	}

	@Remove
	@Destroy
	@Override
	public void destroy() {
		this.log.debug("destroy");
	}
}
