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

package be.fedict.eid.dss.model.bean;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.eid.applet.service.spi.IdentityIntegrityService;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.Constants;
import be.fedict.eid.dss.model.TrustValidationService;
import be.fedict.trust.client.XKMS2Client;
import be.fedict.trust.client.exception.ValidationFailedException;

/**
 * eID Applet Service Identity Integrity Service implementation.
 * 
 * @author Wim Vandenhaute
 */
@Stateless
@Local(IdentityIntegrityService.class)
@LocalBinding(jndiBinding = Constants.DSS_JNDI_CONTEXT
		+ "IdentityIntegrityServiceBean")
public class IdentityIntegrityServiceBean implements IdentityIntegrityService {

	private static final Log LOG = LogFactory
			.getLog(IdentityIntegrityServiceBean.class);

	@EJB
	private Configuration configuration;

	@EJB
	private TrustValidationService trustValidationService;

	public void checkNationalRegistrationCertificate(
			List<X509Certificate> certificateChain) throws SecurityException {

		LOG.debug("validate national registry certificate: "
				+ certificateChain.get(0).getSubjectX500Principal());

		XKMS2Client xkms2Client = this.trustValidationService.getXkms2Client();

		String xkmsUrl = this.configuration.getValue(ConfigProperty.XKMS_URL,
				String.class);
		if (null == xkmsUrl || xkmsUrl.trim().isEmpty()) {
			LOG.warn("no XKMS URL configured!");
			return;
		}

		String xkmsTrustDomain = this.configuration.getValue(
				ConfigProperty.IDENTITY_TRUST_DOMAIN, String.class);
		if (null != xkmsTrustDomain && xkmsTrustDomain.trim().isEmpty()) {
			xkmsTrustDomain = null;
		}
		LOG.debug("Trust domain=" + xkmsTrustDomain);

		try {
			LOG.debug("validating certificate chain");
			if (null != xkmsTrustDomain) {
				xkms2Client.validate(xkmsTrustDomain, certificateChain);
			} else {
				xkms2Client.validate(certificateChain);
			}
		} catch (ValidationFailedException e) {
			LOG.warn("invalid certificate");
			throw new SecurityException("invalid certificate");
		} catch (Exception e) {
			LOG.warn("eID Trust Service error: " + e.getMessage(), e);
			throw new SecurityException("eID Trust Service error");
		}
	}
}
