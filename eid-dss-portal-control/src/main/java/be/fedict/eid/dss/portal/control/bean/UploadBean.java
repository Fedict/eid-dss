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
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
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
import be.fedict.eid.dss.portal.control.state.SigningModel;
import be.fedict.eid.dss.portal.control.state.SigningModelRepository;

@Stateful
@Name("dssUpload")
@LocalBinding(jndiBinding = "fedict/eid/dss/portal/UploadBean")
public class UploadBean implements Upload {

	private static final Set<String> supportedFileExtensions;

	static {
		// TODO JanVdB still required?
		supportedFileExtensions = new HashSet<>();

		// XML document container.
		supportedFileExtensions.add("text/xml");

		// Open Document Format
		supportedFileExtensions.add("application/vnd.oasis.opendocument.text");
		supportedFileExtensions.add("application/vnd.oasis.opendocument.spreadsheet");
		supportedFileExtensions.add("application/vnd.oasis.opendocument.presentation");
		supportedFileExtensions.add("application/vnd.oasis.opendocument.graphics");
		supportedFileExtensions.add("application/vnd.oasis.opendocument.chart");
		supportedFileExtensions.add("application/vnd.oasis.opendocument.formula");
		supportedFileExtensions.add("application/vnd.oasis.opendocument.image");

		// Office OpenXML.
		supportedFileExtensions.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		supportedFileExtensions.add("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		supportedFileExtensions.add("application/vnd.openxmlformats-officedocument.presentationml.presentation");
		supportedFileExtensions.add("application/vnd.openxmlformats-officedocument.presentationml.slideshow");

		// PDF
		supportedFileExtensions.add("application/pdf");

		// ZIP containers.
		supportedFileExtensions.add("application/zip");

		// Associated Signature Container (ETSI TS 102 918 v1.1.1)
		supportedFileExtensions.add("application/vnd.etsi.asic-s+zip");
		supportedFileExtensions.add("application/vnd.etsi.asic-s+zip");

		supportedFileExtensions.add("application/vnd.etsi.asic-e+zip");
		supportedFileExtensions.add("application/vnd.etsi.asic-e+zip");
	}

	@Logger
	private Log log;

	@In(value = SigningModelRepository.ATTRIBUTE_SIGNING_MODEL, scope = ScopeType.SESSION, required = false)
	@Out(value = SigningModelRepository.ATTRIBUTE_SIGNING_MODEL, scope = ScopeType.SESSION, required = false)
	private SigningModel signingModel;

	@Override
	public void listener(UploadEvent event) throws IOException {
		UploadItem item = event.getUploadItem();
		log.info("File upload of file {0} with content-type {1} and size {2}", item.getFileName(), item.getContentType(), item.getFileSize());

		if (hasSupportedContentType(item)) {
			this.signingModel = new SigningModel(
					item.getFileName(),
					item.getContentType(),
					getData(item)
			);
		} else {
			this.signingModel = new SigningModel(
					FilenameUtils.getBaseName(item.getFileName()) + ".zip",
					"application/zip",
					storeDocumentInZipFile(item)
			);
		}
	}

	@Override
	public String done() {
		return "done";
	}

	@Remove
	@Destroy
	@Override
	public void destroy() {
	}

	@SuppressWarnings("EjbProhibitedPackageUsageInspection")
	private byte[] getData(UploadItem uploadItem) throws IOException {
		if (uploadItem.isTempFile()) {
			return FileUtils.readFileToByteArray(uploadItem.getFile());
		} else {
			return uploadItem.getData();
		}
	}

	private byte[] storeDocumentInZipFile(UploadItem item) throws IOException {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
			ZipEntry zipEntry = new ZipEntry(item.getFileName());
			zipOutputStream.putNextEntry(zipEntry);
			IOUtils.write(getData(item), zipOutputStream);
		}

		return outputStream.toByteArray();
	}

	private boolean hasSupportedContentType(UploadItem item) {
		return supportedFileExtensions.contains(item.getContentType());
	}
}
