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

package be.fedict.eid.dss.spi;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Interface for Digital Signature Service protocol components.
 * 
 * @author Frank Cornelis
 * 
 */
public interface DSSProtocolService {

	/**
	 * Initializes this component.
	 * 
	 * @param servletContext
	 */
	void init(ServletContext servletContext);

	/**
	 * Handles an incoming request for this protocol.
	 * 
	 * @param request
	 *            the HTTP request.
	 * @param response
	 *            the HTTP response. Can be used if the protocol handler does
	 *            not want to continue via the regular DSS flow.
	 * 
	 * @throws Exception
	 *             in case this protocol service cannot handle the incoming
	 *             request.
	 */
	void handleIncomingRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception;

	/**
	 * Handles the outgoing response to return to the Service Provider web
	 * application.
	 * 
	 * @param httpSession
	 *            the HTTP session context.
	 * @param request
	 *            the HTTP request.
	 * @param response
	 *            the HTTP response.
	 * @return the response object in case a Browser POST should be constructed.
	 *         <code>null</code> in case this protocol service handles the
	 *         response generation itself.
	 * @throws Exception
	 *             in case this protocol service cannot construct the outgoing
	 *             response.
	 */
	BrowserPOSTResponse handleResponse(HttpSession httpSession,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception;
}
