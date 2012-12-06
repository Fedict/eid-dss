/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009 FedICT.
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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.openssl.PEMWriter;

import be.fedict.eid.dss.model.IdentityService;

/**
 * Identity servlet exposing the full certificate chain of the active eID DSS's
 * identity in PEM format.
 * 
 * @author Wim Vandenhaute
 */
public class IdentityServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory.getLog(IdentityServlet.class);

	@EJB
	IdentityService identityService;

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		LOG.debug("doGet");

		response.setContentType("text/plain");
		PrintWriter out = response.getWriter();

		KeyStore.PrivateKeyEntry activeIdentity = identityService
				.findIdentity();
		if (null == activeIdentity) {

			out.print("No active identity eID DSS Identity configured.");

		} else {

			String pemCertificateChain;
			try {
				pemCertificateChain = toPem(activeIdentity
						.getCertificateChain());
			} catch (Exception e) {
				LOG.error(e);
				return;
			}
			out.print(pemCertificateChain);
		}

		out.close();
	}

	private static String toPem(Certificate[] certificateChain) {

		StringWriter buffer = new StringWriter();
		try {
			PEMWriter writer = new PEMWriter(buffer);
			for (Certificate certificate : certificateChain) {
				writer.writeObject(certificate);
			}
			writer.close();
			return buffer.toString();
		} catch (Exception e) {
			throw new RuntimeException("Cannot convert object to "
					+ "PEM format: " + e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(buffer);
		}
	}
}
