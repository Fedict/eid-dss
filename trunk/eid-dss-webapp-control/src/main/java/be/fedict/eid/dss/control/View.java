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

package be.fedict.eid.dss.control;

import java.io.IOException;
import java.io.OutputStream;

import javax.ejb.Local;

@Local
public interface View {

	public static final String LANGUAGE_SESSION_ATTRIBUTE = "Language";
	public static final String RP_SESSION_ATTRIBUTE = "RelyingParty";

	/*
	 * Actions.
	 */
	String cancel();

	String sign();

	void initLanguage();

	/*
	 * Accessors
	 */
	String getRole();

	void setRole(String role);

	boolean getIncludeIdentity();

	void setIncludeIdentity(boolean includeIdentity);

	String getRp();

	boolean isRpLogo();

	void paint(OutputStream stream, Object object) throws IOException;

	long getTimeStamp();

	String getEmail();

	void setEmail(String email);

	boolean getDisplayMailSignedDocument();
	
	boolean isDisableButtons();

	/*
	 * Lifecycle.
	 */
	void destroy();
}
