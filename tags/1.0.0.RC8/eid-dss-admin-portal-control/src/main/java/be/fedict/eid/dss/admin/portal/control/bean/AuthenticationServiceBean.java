/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010 FedICT.
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

package be.fedict.eid.dss.admin.portal.control.bean;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.Local;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.eid.applet.service.spi.AuthenticationService;
import be.fedict.eid.applet.service.spi.CertificateSecurityException;
import be.fedict.eid.applet.service.spi.ExpiredCertificateSecurityException;
import be.fedict.eid.applet.service.spi.RevokedCertificateSecurityException;
import be.fedict.eid.applet.service.spi.TrustCertificateSecurityException;
import be.fedict.eid.dss.admin.portal.control.AdminConstants;

@Stateless
@Local(AuthenticationService.class)
@LocalBinding(jndiBinding = AdminConstants.ADMIN_JNDI_CONTEXT
		+ "AuthenticationServiceBean")
public class AuthenticationServiceBean implements AuthenticationService {

	private static final Log LOG = LogFactory
			.getLog(AuthenticationServiceBean.class);

	@Override
	public void validateCertificateChain(List<X509Certificate> certificateChain)
			throws ExpiredCertificateSecurityException,
			RevokedCertificateSecurityException,
			TrustCertificateSecurityException, CertificateSecurityException,
			SecurityException {
		/*
		 * Admin trust is based on the public key only.
		 */
		LOG.debug("validateCertificateChain");
	}
}
