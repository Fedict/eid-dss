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

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.spi.DSSProtocolService;

/**
 * The main entry point for DSS protocols. This servlet serves as a broker
 * towards the different protocol services. Depending on the context path the
 * request will be delegated towards the correct protocol service.
 * 
 * @author Frank Cornelis
 * 
 */
public class ProtocolEntryServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(ProtocolEntryServlet.class);

	private Map<String, DSSProtocolService> protocolServices;

	private String unknownProtocolPageInitParam;

	private String protocolErrorPageInitParam;

	private String protocolErrorMessageSessionAttributeInitParam;

	private String nextPageInitParam;

	@Override
	public void init(ServletConfig config) throws ServletException {
		/*
		 * Get init-params.
		 */
		this.unknownProtocolPageInitParam = getRequiredInitParameter(config,
				"UnknownProtocolPage");

		this.protocolErrorPageInitParam = getRequiredInitParameter(config,
				"ProtocolErrorPage");
		this.protocolErrorMessageSessionAttributeInitParam = getRequiredInitParameter(
				config, "ProtocolErrorMessageSessionAttribute");

		this.nextPageInitParam = getRequiredInitParameter(config, "NextPage");

		/*
		 * We align the life-cycle of a DSSProtocoLService with the life-cycle
		 * of this servlet.
		 */
		ServletContext servletContext = config.getServletContext();
		Map<String, String> protocolServiceClasses = StartupServletContextListener
				.getProtocolServices(servletContext);
		this.protocolServices = new HashMap<String, DSSProtocolService>();
		for (Map.Entry<String, String> protocolServiceEntry : protocolServiceClasses
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
				LOG
						.error("could not create an instance of the protocol service class: "
								+ protocolServiceClassName);
				continue;
			}
			dssProtocolService.init(servletContext);
			this.protocolServices.put(contextPath, dssProtocolService);
		}
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		handleRequest(request, response);
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		handleRequest(request, response);
	}

	private void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		LOG.debug("handle request");
		LOG.debug("request URI: " + request.getRequestURI());
		LOG.debug("request method: " + request.getMethod());
		LOG.debug("request path info: " + request.getPathInfo());
		LOG.debug("request context path: " + request.getContextPath());
		LOG.debug("request query string: " + request.getQueryString());
		LOG.debug("request path translated: " + request.getPathTranslated());
		String protocolServiceContextPath = request.getPathInfo();

		DSSProtocolService dssProtocolService = this.protocolServices
				.get(protocolServiceContextPath);
		if (null == dssProtocolService) {
			LOG.warn("unsupported protocol: " + protocolServiceContextPath);
			response.sendRedirect(request.getContextPath()
					+ this.unknownProtocolPageInitParam);
			return;
		}

		try {
			dssProtocolService.handleIncomingRequest(request, response);
		} catch (Exception e) {
			LOG.error("protocol error: " + e.getMessage(), e);
			HttpSession httpSession = request.getSession();
			httpSession.setAttribute(
					this.protocolErrorMessageSessionAttributeInitParam, e
							.getMessage());
			response.sendRedirect(request.getContextPath()
					+ this.protocolErrorPageInitParam);
		}

		response
				.sendRedirect(request.getContextPath() + this.nextPageInitParam);
	}

	private String getRequiredInitParameter(ServletConfig config,
			String initParamName) throws ServletException {
		String value = config.getInitParameter(initParamName);
		if (null == value) {
			throw new ServletException(initParamName + " init-param required");
		}
		return value;
	}
}
