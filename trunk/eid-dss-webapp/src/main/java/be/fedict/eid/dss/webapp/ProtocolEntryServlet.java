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
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import javax.ejb.EJB;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.control.View;
import be.fedict.eid.dss.entity.DocumentEntity;
import be.fedict.eid.dss.entity.RPEntity;
import be.fedict.eid.dss.model.AccountingService;
import be.fedict.eid.dss.model.DocumentRepository;
import be.fedict.eid.dss.model.DocumentService;
import be.fedict.eid.dss.model.RPService;
import be.fedict.eid.dss.spi.DSSDocumentService;
import be.fedict.eid.dss.spi.DSSProtocolService;
import be.fedict.eid.dss.spi.DSSRequest;
import be.fedict.eid.dss.spi.SignatureStatus;

/**
 * The main entry point for DSS protocols. This servlet serves as a broker
 * towards the different protocol services. Depending on the context path the
 * request will be delegated towards the correct protocol service.
 * 
 * @author Frank Cornelis
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

	private String exitPageInitParam;

	@EJB
	private DocumentService documentService;

	@EJB
	private RPService rpService;

	@EJB
	AccountingService accountingService;

	public ProtocolEntryServlet() {
		super(true, true);
	}

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
		this.exitPageInitParam = super.getRequiredInitParameter(config,
				"ExitPage");
	}

	private void storeProtocolServiceContextPath(String contextPath,
			HttpSession httpSession) {
		httpSession.setAttribute(
				PROTOCOL_SERVICE_CONTEXT_PATH_SESSION_ATTRIBUTE, contextPath);
	}

	/**
	 * @param httpSession
	 *            the HTTP session
	 * @return the protocol service context path that was used during entry of
	 *         the eID DSS.
	 */
	public static String retrieveProtocolServiceEntryContextPath(
			HttpSession httpSession) {
		return (String) httpSession
				.getAttribute(PROTOCOL_SERVICE_CONTEXT_PATH_SESSION_ATTRIBUTE);
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
			error(request, response, e.getMessage(), e);
			return;
		}

		// try to identify a RP from the domain
		LOG.debug("DSS Request domain: " + dssRequest.getDomain());
		RPEntity rp = this.rpService.find(dssRequest.getDomain());
		if (null != rp) {

			if (!isValid(rp, dssRequest, request, response)) {
				return;
			}

		}

		// get document data, either from artifact map or direct
		byte[] documentData;
		String contentType;
		if (null != dssRequest.getDocumentId()) {

			DocumentEntity document = this.documentService.find(dssRequest
					.getDocumentId());
			if (null == document) {
				error(request, response, "Document not found!", null);
				return;
			}
			documentData = document.getData();
			contentType = document.getContentType();

		} else {
			documentData = dssRequest.getDocumentData();
			contentType = dssRequest.getContentType();
		}

		if (null == documentData || null == contentType) {
			error(request, response, "No document data or content type found.",
					null);
			return;
		}

		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);

		// store artifact if specified for later
		if (null != dssRequest.getDocumentId()) {
			documentRepository.setDocumentId(dssRequest.getDocumentId());
		}

		/*
		 * Check the document format.
		 */
		LOG.debug("document content type: " + contentType);
		documentRepository.setDocumentContentType(contentType);
		DSSDocumentService documentService = super
				.findDocumentService(contentType);
		if (null == documentService) {
			LOG.debug("no document service found for content type: "
					+ contentType);
			documentRepository.setSignatureStatus(SignatureStatus.FILE_FORMAT);
			response.sendRedirect(request.getContextPath()
					+ this.exitPageInitParam);
			return;
		}
		try {
			documentService.checkIncomingDocument(documentData);
		} catch (Exception e) {
			LOG.debug("document verification error: " + e.getMessage(), e);
			documentRepository.setSignatureStatus(SignatureStatus.FILE_FORMAT);
			response.sendRedirect(request.getContextPath()
					+ this.exitPageInitParam);
			return;
		}

		/*
		 * Store the relevant data into the HTTP session document repository.
		 */
		documentRepository.setDocument(documentData);

		/*
		 * i18n
		 */
		String language = dssRequest.getLanguage();
		if (null != language) {
			httpSession.setAttribute(View.LANGUAGE_SESSION_ATTRIBUTE, language);
		} else {
			httpSession.removeAttribute(View.LANGUAGE_SESSION_ATTRIBUTE);
		}

		// accounting
		accountingService.addRequest(dssRequest.getDomain());

		/*
		 * Goto the next eID DSS page.
		 */
		response.sendRedirect(request.getContextPath() + this.nextPageInitParam);
	}

	private boolean isValid(RPEntity rp, DSSRequest dssRequest,
			HttpServletRequest request, HttpServletResponse response)
			throws IOException {

		LOG.debug("found RP: " + rp.getName());

		if (rp.isRequestSigningRequired()) {
			if (null == dssRequest.getServiceCertificateChain()) {
				error(request, response,
						"Request was not signed, which is required for this SP!");
				return false;
			}
		}

		if (null != dssRequest.getServiceCertificateChain()) {

			X509Certificate serviceCertificate = dssRequest
					.getServiceCertificateChain().get(0);

			if (null != serviceCertificate
					&& null != rp.getEncodedCertificate()) {

				LOG.debug("verify service signature certificate fingerprint against "
						+ "RP's configuration...");

				// verify fingerprint
				// TODO: for now first using fingerprint of value of leaf
				// certificate, expand later for service key rollover scenarios.
				try {
					String rpFingerprint = DigestUtils.shaHex(rp
							.getEncodedCertificate());
					String requestFingerPrint = DigestUtils
							.shaHex(serviceCertificate.getEncoded());

					if (!rpFingerprint.equals(requestFingerPrint)) {
						error(request, response,
								"Request was not signed with the correct keystore!");
						return false;
					}
				} catch (CertificateEncodingException e) {
					return false;
				}

			}
		} else {
			LOG.debug("No signature and signature is not required, valid...");
		}

		request.getSession().setAttribute(View.RP_SESSION_ATTRIBUTE, rp);
		return true;
	}

	private void error(HttpServletRequest request,
			HttpServletResponse response, String errorMessage)
			throws IOException {

		error(request, response, errorMessage, null);
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
