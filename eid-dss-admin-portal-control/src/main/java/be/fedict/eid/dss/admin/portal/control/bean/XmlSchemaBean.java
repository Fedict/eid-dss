/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010-2012 FedICT.
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

package be.fedict.eid.dss.admin.portal.control.bean;

import java.io.InputStream;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;

import be.fedict.eid.dss.admin.portal.control.AdminConstants;
import be.fedict.eid.dss.admin.portal.control.XmlSchema;
import be.fedict.eid.dss.entity.XmlSchemaEntity;
import be.fedict.eid.dss.model.XmlSchemaManager;
import be.fedict.eid.dss.model.exception.ExistingXmlSchemaException;
import be.fedict.eid.dss.model.exception.InvalidXmlSchemaException;

@Stateful
@Name("dssXmlSchema")
@LocalBinding(jndiBinding = AdminConstants.ADMIN_JNDI_CONTEXT + "XmlSchemaBean")
public class XmlSchemaBean implements XmlSchema {

	@Logger
	private Log log;

	private InputStream uploadedFile;

	private String revision;

	@DataModel
	private List<XmlSchemaEntity> dssXmlSchemaList;

	@DataModelSelection
	@Out(required = false)
	private XmlSchemaEntity selectedXmlSchema;

	@EJB
	private XmlSchemaManager xmlSchemaManager;

	@In
	private FacesMessages facesMessages;

	@Remove
	@Destroy
	@Override
	public void destroy() {
		this.log.debug("destroy");
	}

	@Override
	public String add() {
		this.log.debug("add #0", this.revision);
		if (null == this.uploadedFile) {
			this.facesMessages.addToControl("file", "missing XML schema");
			return null;
		}
		try {
			this.xmlSchemaManager.add(this.revision, this.uploadedFile);
		} catch (InvalidXmlSchemaException e) {
			this.facesMessages.addToControl("file", "Not a valid XML schema: "
					+ e.getMessage());
			return null;
		} catch (ExistingXmlSchemaException e) {
			this.facesMessages.addToControl("file", "Existing XML schema");
			return null;
		}
		this.revision = null;
		initList();
		return "success";
	}

	@Override
	public void delete() {
		this.log.debug("delete: " + this.selectedXmlSchema.getNamespace());
		this.xmlSchemaManager.delete(this.selectedXmlSchema.getNamespace());
		initList();
	}

	@Override
	public InputStream getUploadedFile() {
		return this.uploadedFile;
	}

	@Override
	public void setUploadedFile(InputStream uploadedFile) {
		this.uploadedFile = uploadedFile;
	}

	@Override
	@Factory("dssXmlSchemaList")
	public void initList() {
		this.dssXmlSchemaList = this.xmlSchemaManager.getXmlSchemas();
	}

	@Override
	public String getRevision() {
		return this.revision;
	}

	@Override
	public void setRevision(String revision) {
		this.revision = revision;
	}

	@Override
	public String view() {
		return "view-schema";
	}
}
