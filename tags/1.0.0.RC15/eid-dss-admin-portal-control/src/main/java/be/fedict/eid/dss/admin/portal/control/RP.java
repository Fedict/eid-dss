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

import java.io.IOException;
import java.io.OutputStream;

import javax.ejb.Local;

import org.richfaces.event.UploadEvent;

@Local
public interface RP {

	/*
	 * Accessors
	 */
	String getSelectedTab();

	void setSelectedTab(String selectedTab);

	void paint(OutputStream stream, Object object) throws IOException;

	long getTimeStamp();

	/*
	 * Listeners.
	 */
	void uploadListener(UploadEvent event) throws IOException;

	void uploadListenerLogo(UploadEvent event) throws IOException;

	/*
	 * Factories
	 */
	void rpListFactory();

	/*
	 * Actions.
	 */
	String add();

	String modify();

	String save();

	String remove();

	String removeCertificate();

	String back();

	/*
	 * Lifecycle.
	 */
	void destroy();

	void postConstruct();
}
