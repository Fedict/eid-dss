/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009-2012 FedICT.
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

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;

import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.protocol.simple.client.SignatureResponseService;

@Stateless
@Local(SignatureResponseService.class)
@LocalBinding(jndiBinding = "fedict/eid/dss/portal/SignatureResponseServiceBean")
public class SignatureResponseServiceBean implements SignatureResponseService {

	@EJB
	private Configuration configuration;

	@Override
	public String getDssWSUrl() {
		String dssWSUrl = this.configuration.getValue(
				ConfigProperty.DSS_WS_URL, String.class);
		return dssWSUrl;
	}
}
