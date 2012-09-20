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

package be.fedict.eid.dss.protocol.simple.client;

import java.io.IOException;
import java.security.KeyStore;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The signature request servlet can be used for constructing the eID DSS
 * signing request message.
 * <p/>
 * The configuration init-params are documented within the eID DSS developer's
 * guide.
 * 
 * @author Frank Cornelis
 * 
 */
public class SignatureRequestServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(SignatureRequestServlet.class);

	// Request
	public static final String TARGET_SESSION_ATTRIBUTE_INIT_PARAM = "TargetSessionAttribute";
	public static final String SIGNATURE_REQUEST_SESSION_ATTRIBUTE_INIT_PARAM = "SignatureRequestSessionAttribute";
	public static final String SIGNATURE_REQUEST_ID_SESSION_ATTRIBUTE_INIT_PARAM = "SignatureRequestIdSessionAttribute";
	public static final String RELAY_STATE_SESSION_ATTRIBUTE_INIT_PARAM = "RelayStateSessionAttribute";
	public static final String CONTENT_TYPE_SESSION_ATTRIBUTE_INIT_PARAM = "ContentTypeSessionAttribute";

	private static final String SIGNATURE_REQUEST_SERVICE_PARAM = "SignatureRequestService";

	private static final String SP_DESTINATION_PARAM = "SPDestination";
	private static final String SP_DESTINATION_PAGE_PARAM = SP_DESTINATION_PARAM
			+ "Page";
	private static final String TARGET_PARAM = "Target";
	private static final String LANGUAGE_PARAM = "Language";

	// Request config
	private String targetSessionAttribute;
	private String signatureRequestSessionAttribute;
	private String signatureRequestIdSessionAttribute;
	private String relayStateSessionAttribute;
	private String contentTypeSessionAttribute;

	private String spDestination;
	private String spDestinationPage;
	private String target;
	private String language;

	private ServiceLocator<SignatureRequestService> signatureRequestServiceServiceLocator;

	@Override
	public void init(ServletConfig config) throws ServletException {

		LOG.debug("init");
		this.spDestination = config.getInitParameter(SP_DESTINATION_PARAM);
		this.spDestinationPage = config
				.getInitParameter(SP_DESTINATION_PAGE_PARAM);
		this.target = config.getInitParameter(TARGET_PARAM);
		this.language = config.getInitParameter(LANGUAGE_PARAM);

		this.signatureRequestServiceServiceLocator = new ServiceLocator<SignatureRequestService>(
				SIGNATURE_REQUEST_SERVICE_PARAM, config);

		// Request Config
		this.targetSessionAttribute = config
				.getInitParameter(TARGET_SESSION_ATTRIBUTE_INIT_PARAM);
		this.signatureRequestSessionAttribute = config
				.getInitParameter(SIGNATURE_REQUEST_SESSION_ATTRIBUTE_INIT_PARAM);
		this.signatureRequestIdSessionAttribute = config
				.getInitParameter(SIGNATURE_REQUEST_ID_SESSION_ATTRIBUTE_INIT_PARAM);
		this.relayStateSessionAttribute = config
				.getInitParameter(RELAY_STATE_SESSION_ATTRIBUTE_INIT_PARAM);
		this.contentTypeSessionAttribute = config
				.getInitParameter(CONTENT_TYPE_SESSION_ATTRIBUTE_INIT_PARAM);

		// validate necessary configuration params
		if (null == this.target
				&& !this.signatureRequestServiceServiceLocator.isConfigured()) {
			throw new ServletException("need to provide either " + TARGET_PARAM
					+ " or " + SIGNATURE_REQUEST_SERVICE_PARAM
					+ "(Class) init-params");
		}

		if (null == this.spDestination && null == this.spDestinationPage
				&& !this.signatureRequestServiceServiceLocator.isConfigured()) {
			throw new ServletException("need to provide either "
					+ SP_DESTINATION_PARAM + " or " + SP_DESTINATION_PAGE_PARAM
					+ " or " + SIGNATURE_REQUEST_SERVICE_PARAM
					+ "(Class) init-param");
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		LOG.debug("doGet");

		String signatureRequest = (String) request.getSession().getAttribute(
				this.signatureRequestSessionAttribute);
		String signatureRequestId = (String) request.getSession().getAttribute(
				this.signatureRequestIdSessionAttribute);
		String contentType = (String) request.getSession().getAttribute(
				this.contentTypeSessionAttribute);

		String dssDestination;
		String relayState;
		KeyStore.PrivateKeyEntry spIdentity = null;
		String language;

		SignatureRequestService service = this.signatureRequestServiceServiceLocator
				.locateService();
		if (null != service) {
			dssDestination = service.getDssDestination();
			relayState = service.getRelayState(request.getParameterMap());
			spIdentity = service.getSPIdentity();
			language = service.getLanguage();
		} else {
			dssDestination = this.target;
			relayState = (String) request.getSession().getAttribute(
					this.relayStateSessionAttribute);
			language = this.language;
		}

		// sp-destination
		String spDestination = null;
		if (null != service) {
			spDestination = service.getSPDestination();
		}
		if (null == spDestination) {
			// not provided by the service, check web.xml...
			if (null != this.spDestination) {
				spDestination = this.spDestination;
			} else {
				spDestination = request.getScheme() + "://"
						+ request.getServerName() + ":"
						+ request.getServerPort() + request.getContextPath()
						+ this.spDestinationPage;
			}
		}

		// generate and send a signature request
		try {
			SignatureRequestUtil.sendRequest(signatureRequest,
					signatureRequestId, contentType, dssDestination,
					spDestination, relayState, spIdentity, response, language);
		} catch (Exception e) {
			throw new ServletException(e);
		}

		// save state on session
		if (null != relayState) {
			setRelayState(relayState, request.getSession());
		}
		setTarget(spDestination, request.getSession());

	}

	private void setRelayState(String relayState, HttpSession session) {
		session.setAttribute(this.relayStateSessionAttribute, relayState);
	}

	private void setTarget(String target, HttpSession session) {
		session.setAttribute(this.targetSessionAttribute, target);
	}
}
