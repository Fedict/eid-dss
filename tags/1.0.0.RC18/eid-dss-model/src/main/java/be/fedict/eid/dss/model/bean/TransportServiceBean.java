/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2012 FedICT.
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

package be.fedict.eid.dss.model.bean;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.eid.applet.service.spi.StrictTransportSecurityConfig;
import be.fedict.eid.applet.service.spi.TransportService;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.Constants;

@Stateless
@Local(TransportService.class)
@LocalBinding(jndiBinding = Constants.DSS_JNDI_CONTEXT + "TransportServiceBean")
public class TransportServiceBean implements TransportService {

	private static final Log LOG = LogFactory
			.getLog(TransportServiceBean.class);

	@EJB
	private Configuration configuration;

	public StrictTransportSecurityConfig getStrictTransportSecurityConfig() {
		Boolean hsts = this.configuration.getValue(
				ConfigProperty.SECURITY_HSTS, Boolean.class);
		LOG.debug("HSTS: " + hsts);
		if (null == hsts) {
			return null;
		}
		if (false == hsts) {
			return null;
		}
		return new StrictTransportSecurityConfig(365 * 24 * 60 * 60, true);
	}
}
