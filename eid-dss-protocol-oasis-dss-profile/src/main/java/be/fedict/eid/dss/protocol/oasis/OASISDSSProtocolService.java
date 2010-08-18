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

package be.fedict.eid.dss.protocol.oasis;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.spi.BrowserPOSTResponse;
import be.fedict.eid.dss.spi.DSSProtocolService;

/**
 * Class implementing the OASIS DSS Browser POST Profile.
 * 
 * @author Frank Cornelis
 * 
 */
public class OASISDSSProtocolService implements DSSProtocolService {

	private static final Log LOG = LogFactory
			.getLog(OASISDSSProtocolService.class);

	public void handleIncomingRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		LOG.debug("handleIncomingRequest");
	}

	public BrowserPOSTResponse handleResponse(HttpSession httpSession,
			HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		LOG.debug("handleResponse");
		return null;
	}

	public void init(ServletContext servletContext) {
		LOG.debug("init");
	}
}
