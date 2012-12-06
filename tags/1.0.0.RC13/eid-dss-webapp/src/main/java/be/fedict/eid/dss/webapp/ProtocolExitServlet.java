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

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.model.DocumentRepository;
import be.fedict.eid.dss.model.DocumentService;
import be.fedict.eid.dss.model.MailManager;
import be.fedict.eid.dss.model.exception.DocumentNotFoundException;
import be.fedict.eid.dss.spi.BrowserPOSTResponse;
import be.fedict.eid.dss.spi.DSSProtocolService;
import be.fedict.eid.dss.spi.SignatureStatus;

/**
 * Protocol Exit Servlet. Operates as a broker towards protocol services.
 * 
 * @author Frank Cornelis
 */
public class ProtocolExitServlet extends AbstractProtocolServiceServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory.getLog(ProtocolExitServlet.class);

	private String protocolErrorPageInitParam;

	private String protocolErrorMessageSessionAttributeInitParam;

	private String protocolResponsePostPageInitParam;

	private String responseActionSessionAttributeInitParam;

	private String responseAttributesSessionAttributeInitParam;

	@EJB
	private DocumentService documentService;

	@EJB
	private MailManager mailManager;

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
			error(request, response, "no protocol service active", null);
			return;
		}

		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);

		byte[] signedDocument = documentRepository.getSignedDocument();

		if (null != signedDocument) {
			String mimetype = documentRepository.getDocumentContentType();
			String email = documentRepository.getEmail();
			this.mailManager.sendSignedDocument(email, "en", mimetype,
					signedDocument);
		}

		String documentId = documentRepository.getDocumentId();
		if (null != documentId && null != signedDocument) {

			// update document entry
			try {
				this.documentService.update(documentId, signedDocument);
			} catch (DocumentNotFoundException e) {
				error(request, response, "Document not found!", null);
				return;
			}
		} else if (null != documentId) {

			// document artifact needs to be removed, user cancelled signing...
			this.documentService.remove(documentId);
		}

		SignatureStatus signatureStatus = documentRepository
				.getSignatureStatus();
		X509Certificate signerCertificate = documentRepository
				.getSignerCertificate();

		BrowserPOSTResponse returnResponse;
		try {
			returnResponse = protocolService.handleResponse(signatureStatus,
					signedDocument, documentId, signerCertificate, httpSession,
					request, response);
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
		/*
		 * Clean-up the session here as it is no longer used after this point.
		 */
		httpSession.invalidate();
	}

	private void error(HttpServletRequest request,
			HttpServletResponse response, String errorMessage, Throwable t)
			throws IOException {

		LOG.error("Protocol error: " + errorMessage, t);
		request.getSession().setAttribute(
				this.protocolErrorMessageSessionAttributeInitParam,
				errorMessage);
		response.sendRedirect(request.getContextPath()
				+ this.protocolErrorPageInitParam);
	}
}
