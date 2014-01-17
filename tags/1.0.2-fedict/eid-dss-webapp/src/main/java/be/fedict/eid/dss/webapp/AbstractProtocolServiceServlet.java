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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.IdentityService;
import be.fedict.eid.dss.model.TrustValidationService;
import be.fedict.eid.dss.model.XmlSchemaManager;
import be.fedict.eid.dss.model.XmlStyleSheetManager;
import be.fedict.eid.dss.model.bean.ModelDSSDocumentContext;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.DSSDocumentService;
import be.fedict.eid.dss.spi.DSSProtocolContext;
import be.fedict.eid.dss.spi.DSSProtocolService;

/**
 * The base class for servlets that need to use various services. Manages the
 * life-cycle of the protocol services and the document services.
 * 
 * @author Frank Cornelis
 */
public abstract class AbstractProtocolServiceServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(AbstractProtocolServiceServlet.class);

	private Map<String, DSSProtocolService> protocolServices;

	private Map<String, DSSDocumentService> documentServices;

	private final boolean initDocumentServices;

	private final boolean initProtocolServices;

	@EJB
	private IdentityService identityService;

	@EJB
	private XmlSchemaManager xmlSchemaManager;

	@EJB
	private XmlStyleSheetManager xmlStyleSheetManager;

	@EJB
	private TrustValidationService trustValidationService;

	@EJB
	private Configuration configuration;

	/**
	 * Main constructor.
	 * 
	 * @param initProtocolServices
	 *            initialize protocol services or not
	 * @param initDocumentServices
	 *            initialize document services or not
	 */
	protected AbstractProtocolServiceServlet(boolean initProtocolServices,
			boolean initDocumentServices) {
		this.initProtocolServices = initProtocolServices;
		this.initDocumentServices = initDocumentServices;
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		/*
		 * We align the life-cycle of a DSSProtocolService and
		 * DSSDocumentService with the life-cycle of this servlet.
		 */
		ServletContext servletContext = config.getServletContext();

		initializeProtocolServices(servletContext);

		initializeDocumentServices(servletContext);
	}

	@SuppressWarnings("unchecked")
	private void initializeProtocolServices(ServletContext servletContext) {

		if (!this.initProtocolServices) {
			return;
		}
		DSSProtocolContext dssProtocolContext = new DSSProtocolContextImpl(
				this.identityService);
		Map<String, String> protocolServiceClassNames = StartupServletContextListener
				.getProtocolServiceClassNames(servletContext);
		this.protocolServices = new HashMap<String, DSSProtocolService>();
		for (Map.Entry<String, String> protocolServiceEntry : protocolServiceClassNames
				.entrySet()) {
			String contextPath = protocolServiceEntry.getKey();
			String protocolServiceClassName = protocolServiceEntry.getValue();
			Class<? extends DSSProtocolService> protocolServiceClass;
			try {
				protocolServiceClass = (Class<? extends DSSProtocolService>) Class
						.forName(protocolServiceClassName);
			} catch (ClassNotFoundException e) {
				LOG.error("protocol service class not found: "
						+ protocolServiceClassName);
				continue;
			}
			DSSProtocolService dssProtocolService;
			try {
				dssProtocolService = protocolServiceClass.newInstance();
			} catch (Exception e) {
				LOG.error("could not create an instance of the protocol service class: "
						+ protocolServiceClassName);
				continue;
			}
			dssProtocolService.init(servletContext, dssProtocolContext);
			this.protocolServices.put(contextPath, dssProtocolService);
		}
	}

	@SuppressWarnings("unchecked")
	private void initializeDocumentServices(ServletContext servletContext) {

		if (!this.initDocumentServices) {
			return;
		}
		DSSDocumentContext dssDocumentContext = new ModelDSSDocumentContext(
				this.xmlSchemaManager, this.xmlStyleSheetManager,
				this.trustValidationService, this.configuration);
		this.documentServices = new HashMap<String, DSSDocumentService>();
		Map<String, String> documentServiceClassNames = StartupServletContextListener
				.getDocumentServiceClassNames(servletContext);
		for (Map.Entry<String, String> documentServiceEntry : documentServiceClassNames
				.entrySet()) {
			String contentType = documentServiceEntry.getKey();
			String documentServiceClassName = documentServiceEntry.getValue();
			Class<? extends DSSDocumentService> documentServiceClass;
			try {
				documentServiceClass = (Class<? extends DSSDocumentService>) Class
						.forName(documentServiceClassName);
			} catch (ClassNotFoundException e) {
				LOG.error("document service class not found: "
						+ documentServiceClassName);
				continue;
			}
			DSSDocumentService dssDocumentService;
			try {
				dssDocumentService = documentServiceClass.newInstance();
			} catch (Exception e) {
				LOG.error("could not create an instance of the document service class: "
						+ documentServiceClassName);
				continue;
			}
			try {
				dssDocumentService.init(dssDocumentContext, contentType);
			} catch (Exception e) {
				LOG.error(
						"error initializing document service: "
								+ e.getMessage(), e);
			}
			this.documentServices.put(contentType, dssDocumentService);
		}
	}

	@Override
	protected final void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		handleRequest(request, response);
	}

	@Override
	protected final void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		handleRequest(request, response);
	}

	protected abstract void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException;

	/**
	 * Gives back the protocol service for the given protocol context path.
	 * 
	 * @param contextPath
	 *            protocol context path to get service for
	 * @return the protocol service for this context path
	 */
	protected DSSProtocolService findProtocolService(String contextPath) {

		if (!this.initProtocolServices) {
			throw new RuntimeException("protocol services not initialized");
		}
		return this.protocolServices.get(contextPath);
	}

	protected String getRequiredInitParameter(ServletConfig config,
			String initParamName) throws ServletException {
		String value = config.getInitParameter(initParamName);
		if (null == value) {
			throw new ServletException(initParamName + " init-param required");
		}
		return value;
	}

	protected DSSDocumentService findDocumentService(String contentType) {

		if (!this.initDocumentServices) {
			throw new RuntimeException("document services not initialized");
		}
		return this.documentServices.get(contentType);
	}
}
