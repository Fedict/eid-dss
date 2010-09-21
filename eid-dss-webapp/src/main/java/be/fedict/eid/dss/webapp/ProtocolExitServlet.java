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
import java.security.cert.X509Certificate;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.model.DocumentRepository;
import be.fedict.eid.dss.spi.BrowserPOSTResponse;
import be.fedict.eid.dss.spi.DSSProtocolService;
import be.fedict.eid.dss.spi.SignatureStatus;

/**
 * Protocol Exit Servlet. Operates as a broker towards protocol services.
 * 
 * @author Frank Cornelis
 * 
 */
public class ProtocolExitServlet extends AbstractProtocolServiceServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory.getLog(ProtocolExitServlet.class);

	private String protocolErrorPageInitParam;

	private String protocolErrorMessageSessionAttributeInitParam;

	private String protocolResponsePostPageInitParam;

	private String responseActionSessionAttributeInitParam;

	private String responseAttributesSessionAttributeInitParam;

	public ProtocolExitServlet() {
		super(true, false);
	}

	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);

		this.protocolErrorPageInitParam = super.getRequiredInitParameter(
				config, "ProtocolErrorPage");
		this.protocolErrorMessageSessionAttributeInitParam = super
				.getRequiredInitParameter(config,
						"ProtocolErrorMessageSessionAttribute");

		this.protocolResponsePostPageInitParam = super
				.getRequiredInitParameter(config, "ProtocolResponsePostPage");
		this.responseActionSessionAttributeInitParam = super
				.getRequiredInitParameter(config,
						"ResponseActionSessionAttribute");
		this.responseAttributesSessionAttributeInitParam = super
				.getRequiredInitParameter(config,
						"ResponseAttributesSessionAttribute");
	}

	@Override
	protected void handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		LOG.debug("doGet");
		HttpSession httpSession = request.getSession();
		String entryContextPath = ProtocolEntryServlet
				.retrieveProtocolServiceEntryContextPath(httpSession);
		DSSProtocolService protocolService = super
				.findProtocolService(entryContextPath);
		if (null == protocolService) {
			String msg = "no protocol service active";
			LOG.error(msg);
			httpSession.setAttribute(
					this.protocolErrorMessageSessionAttributeInitParam, msg);
			response.sendRedirect(request.getContextPath()
					+ this.protocolErrorPageInitParam);
			return;
		}

		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		byte[] signedDocument = documentRepository.getSignedDocument();
		SignatureStatus signatureStatus = documentRepository
				.getSignatureStatus();
		X509Certificate signerCertificate = documentRepository
				.getSignerCertificate();
		BrowserPOSTResponse returnResponse;
		try {
			returnResponse = protocolService.handleResponse(signatureStatus,
					signedDocument, signerCertificate, httpSession, request,
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
		if (null != returnResponse) {
			/*
			 * This means that the protocol service wants us to construct some
			 * Browser POST response towards the Service Provider landing site.
			 */
			LOG.debug("constructing generic Browser POST response...");
			httpSession.setAttribute(
					this.responseActionSessionAttributeInitParam,
					returnResponse.getActionUrl());
			httpSession.setAttribute(
					this.responseAttributesSessionAttributeInitParam,
					returnResponse.getAttributes());
			response.sendRedirect(request.getContextPath()
					+ this.protocolResponsePostPageInitParam);
			return;
		}
		LOG.debug("protocol service managed its own protocol response");
	}
}
