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

package be.fedict.eid.dss.sp.bean;

import java.io.Serializable;
import java.security.KeyStore;
import java.util.Map;
import java.util.UUID;

import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.protocol.simple.client.SignatureRequestService;
import be.fedict.eid.dss.sp.servlet.PkiServlet;

public class SignatureRequestServiceBean implements SignatureRequestService,
		Serializable {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(SignatureRequestServiceBean.class);

	@Override
	public String getSPDestination() {

		HttpServletRequest httpServletRequest = getHttpServletRequest();

		return httpServletRequest.getScheme() + "://"
				+ httpServletRequest.getServerName() + ":"
				+ httpServletRequest.getServerPort()
				+ httpServletRequest.getContextPath() + "/dss-response";

		// return "../eid-dss-sp/dss-response";
	}

	@Override
	public String getDssDestination() {
		return "../eid-dss/protocol/simple";
	}

	@Override
	public String getRelayState(Map<String, String[]> parameterMap) {
		return UUID.randomUUID().toString();
	}

	@Override
	public KeyStore.PrivateKeyEntry getSPIdentity() {

		LOG.debug("get SP Identity");
		try {
			KeyStore.PrivateKeyEntry pke = PkiServlet.getPrivateKeyEntry();
			LOG.debug("certificate: " + pke.getCertificate());
			return pke;
		} catch (Exception e) {
			LOG.error(e);
			return null;
		}

	}

	@Override
	public String getLanguage() {
		return "fr";
	}

	private static HttpServletRequest getHttpServletRequest() {
		HttpServletRequest httpServletRequest;
		try {
			httpServletRequest = (HttpServletRequest) PolicyContext
					.getContext("javax.servlet.http.HttpServletRequest");
		} catch (PolicyContextException e) {
			throw new RuntimeException("JACC error: " + e.getMessage());
		}

		return httpServletRequest;
	}

}
