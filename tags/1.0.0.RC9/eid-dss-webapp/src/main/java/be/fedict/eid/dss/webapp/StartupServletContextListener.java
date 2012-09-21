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

import be.fedict.eid.dss.model.DocumentService;
import be.fedict.eid.dss.model.ServicesManager;
import be.fedict.eid.dss.model.exception.InvalidCronExpressionException;

/**
 * Startup servlet component. This servlet context listener boots up the eID DSS
 * system.
 * 
 * @author Frank Cornelis
 */
public class StartupServletContextListener implements ServletContextListener {

	private static final Log LOG = LogFactory
			.getLog(StartupServletContextListener.class);

	private static final String PROTOCOL_SERVICES_CONTEXT_ATTRIBUTE = StartupServletContextListener.class
			.getName() + ".ProtocolServices";

	private static final String DOCUMENT_SERVICES_CONTEXT_ATTRIBUTE = StartupServletContextListener.class
			.getName() + ".DocumentServices";

	@EJB
	private ServicesManager servicesManager;

	@EJB
	private DocumentService documentService;

	public void contextInitialized(ServletContextEvent event) {

		LOG.debug("contextInitialized");

		initCleanupTask();

		initProtocolServices(event);

		initDocumentServices(event);
	}

	public void contextDestroyed(ServletContextEvent event) {
		LOG.debug("contextDestroyed");
	}

	private void initCleanupTask() {

		LOG.debug("init cleanup task");

		try {
			this.documentService.startTimer();
		} catch (InvalidCronExpressionException e) {
			throw new RuntimeException(e);
		}
	}

	private void initProtocolServices(ServletContextEvent event) {

		/*
		 * We once load the protocol services so we don't have to iterate over
		 * all protocol descriptor files upon each DSS request.
		 */
		ServletContext servletContext = event.getServletContext();
		Map<String, String> protocolServiceClassNames = this.servicesManager
				.getProtocolServiceClassNames();
		servletContext.setAttribute(PROTOCOL_SERVICES_CONTEXT_ATTRIBUTE,
				protocolServiceClassNames);
	}

	private void initDocumentServices(ServletContextEvent event) {

		ServletContext servletContext = event.getServletContext();
		Map<String, String> documentServiceClassNames = this.servicesManager
				.getDocumentServiceClassNames();
		servletContext.setAttribute(DOCUMENT_SERVICES_CONTEXT_ATTRIBUTE,
				documentServiceClassNames);
	}

	/**
	 * @param context
	 *            the servlet context
	 * @return map of protocol services with context path as key. This map has
	 *         been constructed during startup of the eID DSS service.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getProtocolServiceClassNames(
			ServletContext context) {
		return (Map<String, String>) context
				.getAttribute(PROTOCOL_SERVICES_CONTEXT_ATTRIBUTE);
	}

	/**
	 * @param context
	 *            the servlet context
	 * @return map of document service with content type as key. This map has
	 *         been constructed during startup of the eID DSS service.
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, String> getDocumentServiceClassNames(
			ServletContext context) {
		return (Map<String, String>) context
				.getAttribute(DOCUMENT_SERVICES_CONTEXT_ATTRIBUTE);
	}
}
