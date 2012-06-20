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

package be.fedict.eid.dss.admin.portal.control;

import java.util.List;

import javax.ejb.Local;
import javax.faces.model.SelectItem;

@Local
public interface Identity {

	/*
	 * Accessors.
	 */
	String getIdentityLabel();

	String getName();

	void setName(String name);

	Boolean isNameReadOnly();

	String getKeyStoreType();

	void setKeyStoreType(String keyStoreType);

	String getKeyStorePath();

	void setKeyStorePath(String keyStorePath);

	String getKeyStorePassword();

	void setKeyStorePassword(String keyStorePassword);

	String getKeyEntryPassword();

	void setKeyEntryPassword(String keyEntryPassword);

	String getKeyEntryAlias();

	void setKeyEntryAlias(String keyEntryAlias);

	boolean isActive();

	/*
	 * Factories
	 */
	List<SelectItem> getIdentityNames();

	List<SelectItem> keyStoreTypeFactory();

	/*
	 * Actions.
	 */
	String save();

	String activate();

	String remove();

	String test();

	/*
	 * Lifecycle.
	 */
	void destroy();

	void create();
}
