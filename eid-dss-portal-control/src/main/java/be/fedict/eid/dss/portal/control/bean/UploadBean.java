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

import java.io.IOException;

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
import be.fedict.eid.dss.portal.control.state.SigningModel;
import be.fedict.eid.dss.portal.control.state.SigningModelRepository;

@Stateful
@Name("dssUpload")
@LocalBinding(jndiBinding = "fedict/eid/dss/portal/UploadBean")
public class UploadBean implements Upload {

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
				item.getContentType(),
				getData(item)
		);
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
