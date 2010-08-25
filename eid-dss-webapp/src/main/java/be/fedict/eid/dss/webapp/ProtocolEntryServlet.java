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

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.control.XMLView;
import be.fedict.eid.dss.model.DocumentRepository;
import be.fedict.eid.dss.spi.DSSProtocolService;
import be.fedict.eid.dss.spi.DSSRequest;

/**
 * The main entry point for DSS protocols. This servlet serves as a broker
 * towards the different protocol services. Depending on the context path the
 * request will be delegated towards the correct protocol service.
 * 
 * @author Frank Cornelis
 * 
 */
public class ProtocolEntryServlet extends AbstractProtocolServiceServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(ProtocolEntryServlet.class);

	private static final String PROTOCOL_SERVICE_CONTEXT_PATH_SESSION_ATTRIBUTE = ProtocolEntryServlet.class
			.getName() + ".ProtocolServiceContextPath";

	private String unknownProtocolPageInitParam;

	private String protocolErrorPageInitParam;

	private String protocolErrorMessageSessionAttributeInitParam;

	private String nextPageInitParam;

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		this.unknownProtocolPageInitParam = super.getRequiredInitParameter(
				config, "UnknownProtocolPage");

		this.protocolErrorPageInitParam = super.getRequiredInitParameter(
				config, "ProtocolErrorPage");
		this.protocolErrorMessageSessionAttributeInitParam = super
				.getRequiredInitParameter(config,
						"ProtocolErrorMessageSessionAttribute");

		this.nextPageInitParam = super.getRequiredInitParameter(config,
				"NextPage");
	}

	private void storeProtocolServiceContextPath(String contextPath,
			HttpSession httpSession) {
		httpSession.setAttribute(
				PROTOCOL_SERVICE_CONTEXT_PATH_SESSION_ATTRIBUTE, contextPath);
	}

	/**
	 * Gives back the protocol service context path that was used during entry
	 * of the eID DSS.
	 * 
	 * @param httpSession
	 * @return
	 */
	public static String retrieveProtocolServiceEntryContextPath(
			HttpSession httpSession) {
		String contextPath = (String) httpSession
				.getAttribute(PROTOCOL_SERVICE_CONTEXT_PATH_SESSION_ATTRIBUTE);
		return contextPath;
	}

	@Override
	protected void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		LOG.debug("handle request");
		LOG.debug("request URI: " + request.getRequestURI());
		LOG.debug("request method: " + request.getMethod());
		LOG.debug("request path info: " + request.getPathInfo());
		LOG.debug("request context path: " + request.getContextPath());
		LOG.debug("request query string: " + request.getQueryString());
		LOG.debug("request path translated: " + request.getPathTranslated());
		String protocolServiceContextPath = request.getPathInfo();
		HttpSession httpSession = request.getSession();
		storeProtocolServiceContextPath(protocolServiceContextPath, httpSession);

		DSSProtocolService dssProtocolService = super
				.findProtocolService(protocolServiceContextPath);
		if (null == dssProtocolService) {
			LOG.warn("unsupported protocol: " + protocolServiceContextPath);
			response.sendRedirect(request.getContextPath()
					+ this.unknownProtocolPageInitParam);
			return;
		}

		DSSRequest dssRequest;
		try {
			dssRequest = dssProtocolService.handleIncomingRequest(request,
					response);
		} catch (Exception e) {
			LOG.error("protocol error: " + e.getMessage(), e);
			httpSession.setAttribute(
					this.protocolErrorMessageSessionAttributeInitParam,
					e.getMessage());
			response.sendRedirect(request.getContextPath()
					+ this.protocolErrorPageInitParam);
			return;
		}

		/*
		 * Store the relevant data into the HTTP session document repository.
		 */
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		documentRepository.setDocument(dssRequest.getDocumentData());

		/*
		 * i18n
		 */
		String language = dssRequest.getLanguage();
		if (null != language) {
			httpSession.setAttribute(XMLView.LANGUAGE_SESSION_ATTRIBUTE,
					language);
		} else {
			httpSession.removeAttribute(XMLView.LANGUAGE_SESSION_ATTRIBUTE);
		}

		/*
		 * Goto the next eID DSS page.
		 */
		response.sendRedirect(request.getContextPath() + this.nextPageInitParam);
	}
}
