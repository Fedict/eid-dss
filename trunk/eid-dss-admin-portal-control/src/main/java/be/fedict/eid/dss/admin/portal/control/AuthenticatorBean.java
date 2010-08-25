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

import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.security.Credentials;
import org.jboss.seam.security.Identity;

@Name("authenticator")
@Stateless
@LocalBinding(jndiBinding = "fedict/eid/dss/admin/portal/AuthenticatorBean")
public class AuthenticatorBean implements Authenticator {

	private static final Log LOG = LogFactory.getLog(AuthenticatorBean.class);

	@In
	Credentials credentials;

	@In
	Identity identity;

	public boolean authenticate() {
		LOG.debug("authenticate: " + this.credentials.getUsername());
		// TODO: check whether the user really has the correct roles.
		this.identity.addRole("admin");
		return true;
	}
}