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

import javax.ejb.Local;
import javax.ejb.Remove;

@Local
public interface CSRForm {

	// accessors
	String getDn();

	void setDn(String dn);

	String getType();

	void setType(String type);

	String getOperatorFunction();

	void setOperatorFunction(String operatorFunction);

	String getOperatorEmail();

	void setOperatorEmail(String operatorEmail);

	String getOperatorPhone();

	void setOperatorPhone(String operatorPhone);

	String getDescription();

	void setDescription(String description);

	String getCsr();

	void setCsr(String csr);

	// actions
	String submit();

	// lifecycle
	@Remove
	void destroy();
}
