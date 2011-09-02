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

import java.security.KeyStore;
import java.util.Map;

/**
 * DSS Simple Protocol SPI for Signature Requests. This allows for runtime
 * configuration of the {@link SignatureRequestServlet}.
 * 
 * @author Wim Vandenhaute
 */
public interface SignatureRequestService {

	/**
	 * Gives back the Service Provider destination endpoint that will handle the
	 * returned DSS Response.
	 * <p/>
	 * If <code>null</code> the <code>SPDestination</code> or
	 * <code>SPDestinationPage</code> init params in web.xml will be used.
	 * 
	 * @return SP DSS response handling location or <code>null</code>.
	 */
	String getSPDestination();

	/**
	 * Gives back the target URL of the eID DSS Simple protocol entry point.
	 * 
	 * @return eID DSS Simple protocol entry point
	 */
	String getDssDestination();

	/**
	 * Gives back the relay state to be used towards the eID DSS Simple protocol
	 * entry point.
	 * 
	 * @param parameterMap
	 *            the HTTP parameter map.
	 * @return relay state
	 */
	String getRelayState(Map<String, String[]> parameterMap);

	/**
	 * Gives back the optional Service Provider's identity to be used to sign
	 * outgoing DSS signature requests.
	 * 
	 * @return private key entry of the SP or <code>null</code> if no signing is
	 *         needed.
	 */
	KeyStore.PrivateKeyEntry getSPIdentity();

	/**
	 * Language hint for the eID DSS webapp. Return <code>null</code> if the
	 * browser's locale is ok.
	 * 
	 * @return language hint for the eID DSS webapp.
	 */
	String getLanguage();
}
