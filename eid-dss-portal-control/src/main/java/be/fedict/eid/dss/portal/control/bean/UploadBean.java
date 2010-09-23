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

import java.io.File;

import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.apache.commons.io.FileUtils;
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

	@In(value = "document", scope = ScopeType.SESSION, required = false)
	@Out(value = "document", scope = ScopeType.SESSION, required = false)
	private byte[] document;

	@Override
	public String done() {
		this.log.debug("done");
		return "done";
	}

	@Override
	public void listener(UploadEvent event) throws Exception {
		this.log.debug("listener");
		UploadItem item = event.getUploadItem();
		this.log.debug("content type: #0", item.getContentType());
		this.log.debug("filename: #0", item.getFileName());
		this.filename = item.getFileName();
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
