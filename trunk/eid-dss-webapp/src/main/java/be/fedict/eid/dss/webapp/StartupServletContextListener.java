/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009-2010 FedICT.
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

package be.fedict.eid.dss.webapp;

import java.util.Map;

import javax.ejb.EJB;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.model.ProtocolServiceManager;

/**
 * Startup servlet component. This servlet context listener boots up the eID DSS
 * system.
 * 
 * @author Frank Cornelis
 * 
 */
public class StartupServletContextListener implements ServletContextListener {

	private static final Log LOG = LogFactory
			.getLog(StartupServletContextListener.class);

	private static final String PROTOCOL_SERVICES_CONTEXT_ATTRIBUTE = StartupServletContextListener.class
			.getName()
			+ ".ProtocolServices";

	@EJB
	private ProtocolServiceManager protocolServiceManager;

	public void contextInitialized(ServletContextEvent event) {
		LOG.debug("contextInitialized");
		ServletContext servletContext = event.getServletContext();
		/*
		 * We once load the protocol services so we don't have to iterate over
		 * all protocol descriptor files upon each DSS request.
		 */
		Map<String, String> protocolServices = this.protocolServiceManager
				.getProtocolServices();
		servletContext.setAttribute(PROTOCOL_SERVICES_CONTEXT_ATTRIBUTE,
				protocolServices);
	}

	public void contextDestroyed(ServletContextEvent event) {
		LOG.debug("contextDestroyed");
	}

	/**
	 * Gives back a map of protocol services with context path as key. This map
	 * has been constructed during startup of the eID DSS service.
	 * 
	 * @param context
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getProtocolServices(ServletContext context) {
		Map<String, String> protocolServices = (Map<String, String>) context
				.getAttribute(PROTOCOL_SERVICES_CONTEXT_ATTRIBUTE);
		return protocolServices;
	}
}
