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

import java.security.cert.X509Certificate;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.spi.BrowserPOSTResponse;
import be.fedict.eid.dss.spi.DSSContext;
import be.fedict.eid.dss.spi.DSSProtocolService;
import be.fedict.eid.dss.spi.DSSRequest;
import be.fedict.eid.dss.spi.SignatureStatus;

/**
 * Class implementing the OASIS DSS Browser POST Profile.
 * 
 * @author Frank Cornelis
 * 
 */
public class OASISDSSProtocolService implements DSSProtocolService {

	private static final long serialVersionUID = 1L;
	private static final Log LOG = LogFactory
			.getLog(OASISDSSProtocolService.class);

	public DSSRequest handleIncomingRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		LOG.debug("handleIncomingRequest");
		// TODO: implement me
		return null;
	}

	public BrowserPOSTResponse handleResponse(SignatureStatus signatureStatus,
			byte[] signedDocument, X509Certificate signerCertificate,
			HttpSession httpSession, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		LOG.debug("handleResponse");
		// TODO: implement me
		return null;
	}

	public void init(ServletContext servletContext, DSSContext dssContext) {
		LOG.debug("init");
	}
}
