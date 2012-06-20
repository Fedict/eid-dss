/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2011 FedICT.
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

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.log.Log;

import be.fedict.eid.dss.admin.portal.control.AdminConstants;
import be.fedict.eid.dss.admin.portal.control.Admins;
import be.fedict.eid.dss.entity.AdministratorEntity;
import be.fedict.eid.dss.model.AdministratorManager;

@Stateful
@Name("dssAdmins")
@LocalBinding(jndiBinding = AdminConstants.ADMIN_JNDI_CONTEXT + "AdminsBean")
public class AdminsBean implements Admins {

	@Logger
	private Log log;

	@EJB
	private AdministratorManager administratorManager;

	@SuppressWarnings("unused")
	@DataModel
	private List<AdministratorEntity> dssAdminList;

	@DataModelSelection
	private AdministratorEntity selectedAdmin;

	@Override
	public void delete() {
		this.log.debug("delete: #0", this.selectedAdmin.getName());
		this.administratorManager.removeAdmin(this.selectedAdmin.getId());
		initList();
	}

	@Override
	public void approve() {
		this.log.debug("approve: #0", this.selectedAdmin.getName());
		this.administratorManager.approveAdmin(this.selectedAdmin.getId());
		initList();
	}

	@Remove
	@Destroy
	@Override
	public void destroy() {
		this.log.debug("destroy");
	}

	@Override
	@Factory("dssAdminList")
	public void initList() {
		this.dssAdminList = this.administratorManager.listAdmins();
	}
}
