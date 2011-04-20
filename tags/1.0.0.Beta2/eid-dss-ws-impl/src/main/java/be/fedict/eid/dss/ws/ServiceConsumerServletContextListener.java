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

package be.fedict.eid.dss.ws;

import javax.ejb.EJB;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.model.SignatureVerificationService;

/**
 * Service Consumer Servlet Context Listener. For the moment this is the only
 * way to retrieve the EJB session bean references in JAX-WS RI under JBoss AS
 * 5.
 * 
 * @author Frank Cornelis
 * 
 */
public class ServiceConsumerServletContextListener implements
		ServletContextListener {

	@EJB
	private SignatureVerificationService signatureVerificationService;

	private static final Log LOG = LogFactory
			.getLog(ServiceConsumerServletContextListener.class);

	public void contextDestroyed(ServletContextEvent event) {
		LOG.debug("context destroyed");
	}

	public void contextInitialized(ServletContextEvent event) {
		LOG.debug("context initialized");
		ServletContext servletContext = event.getServletContext();
		LOG.debug("SignatureVerificationService ref available: "
				+ (null != this.signatureVerificationService));
		servletContext.setAttribute(SignatureVerificationService.class
				.getName(), this.signatureVerificationService);
	}

	public static SignatureVerificationService getSignatureVerificationService(
			ServletContext context) {
		SignatureVerificationService service = (SignatureVerificationService) context
				.getAttribute(SignatureVerificationService.class.getName());
		return service;
	}
}
