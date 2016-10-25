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

import be.fedict.eid.dss.portal.control.Upload;
import be.fedict.eid.dss.portal.control.state.SigningModel;
import be.fedict.eid.dss.portal.control.state.SigningModelRepository;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.*;
import org.jboss.seam.log.Log;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Stateful
@Name("dssUpload")
@LocalBinding(jndiBinding = "fedict/eid/dss/portal/UploadBean")
public class UploadBean implements Upload {

	private static final Map<String, String> extensionsToMimeTypeMappings;

	static {
		extensionsToMimeTypeMappings = new HashMap<>();

		// XML document container.
		extensionsToMimeTypeMappings.put("xml", "text/xml");

		// Open Document Format
		extensionsToMimeTypeMappings.put("odt", "application/vnd.oasis.opendocument.text");
		extensionsToMimeTypeMappings.put("ods", "application/vnd.oasis.opendocument.spreadsheet");
		extensionsToMimeTypeMappings.put("odp", "application/vnd.oasis.opendocument.presentation");
		extensionsToMimeTypeMappings.put("odg", "application/vnd.oasis.opendocument.graphics");
		extensionsToMimeTypeMappings.put("odc", "application/vnd.oasis.opendocument.chart");
		extensionsToMimeTypeMappings.put("odf", "application/vnd.oasis.opendocument.formula");
		extensionsToMimeTypeMappings.put("odi", "application/vnd.oasis.opendocument.image");

		// Office OpenXML.
		extensionsToMimeTypeMappings.put("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		extensionsToMimeTypeMappings.put("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		extensionsToMimeTypeMappings.put("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation");
		extensionsToMimeTypeMappings.put("ppsx", "application/vnd.openxmlformats-officedocument.presentationml.slideshow");

		// ZIP containers.
		extensionsToMimeTypeMappings.put("zip", "application/zip");

		// Associated Signature Container (ETSI TS 102 918 v1.1.1)
		extensionsToMimeTypeMappings.put("asics", "application/vnd.etsi.asic-s+zip");
		extensionsToMimeTypeMappings.put("scs", "application/vnd.etsi.asic-s+zip");

		extensionsToMimeTypeMappings.put("asice", "application/vnd.etsi.asic-e+zip");
		extensionsToMimeTypeMappings.put("sce", "application/vnd.etsi.asic-e+zip");
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

		this.signingModel = new SigningModel(
				item.getFileName(),
				determineContentType(item),
				getData(item)
		);
	}

	private String determineContentType(UploadItem item) {
		String extension = FilenameUtils.getExtension(item.getFileName()).toLowerCase();
		String contentType = extensionsToMimeTypeMappings.get(extension);
		if (contentType != null) {
			return contentType;
		}

		return item.getContentType();
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
}
