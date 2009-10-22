/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009 FedICT.
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

package be.fedict.eid.dss.portal.model;

import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;

@Stateful
@Name("csrForm")
@LocalBinding(jndiBinding = "fedict/eid/dss/portal/CSRFormBean")
public class CSRFormBean implements CSRForm {

	@Logger
	private Log log;

	private String csr;

	private String description;

	private String type;

	private String dn;

	private String operatorFunction;

	private String operatorPhone;

	private String operatorEmail;

	@Remove
	@Destroy
	public void destroy() {
		this.log.debug("destroy");
	}

	public String getCsr() {
		return this.csr;
	}

	public String getDescription() {
		return this.description;
	}

	public String getDn() {
		return this.dn;
	}

	public String getOperatorEmail() {
		return this.operatorEmail;
	}

	public String getOperatorFunction() {
		return this.operatorFunction;
	}

	public String getOperatorPhone() {
		return this.operatorPhone;
	}

	public String getType() {
		return this.type;
	}

	public void setCsr(String csr) {
		this.csr = csr;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public void setOperatorEmail(String operatorEmail) {
		this.operatorEmail = operatorEmail;
	}

	public void setOperatorFunction(String operatorFunction) {
		this.operatorFunction = operatorFunction;
	}

	public void setOperatorPhone(String operatorPhone) {
		this.operatorPhone = operatorPhone;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String submit() {
		this.log.debug("submit");
		// TODO
		return "success";
	}

}
