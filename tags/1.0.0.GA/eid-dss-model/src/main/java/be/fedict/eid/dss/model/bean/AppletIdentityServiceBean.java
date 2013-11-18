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

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.ejb3.annotation.LocalBinding;

import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;
import be.fedict.eid.applet.service.spi.IdentityRequest;
import be.fedict.eid.applet.service.spi.IdentityService;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.Constants;
import be.fedict.eid.dss.model.DocumentRepository;

@Stateless
@Local(IdentityService.class)
@LocalBinding(jndiBinding = Constants.DSS_JNDI_CONTEXT
		+ "AppletIdentityServiceBean")
public class AppletIdentityServiceBean implements IdentityService {

	private static final Log LOG = LogFactory
			.getLog(AppletIdentityServiceBean.class);

	@EJB
	private Configuration configuration;

	public IdentityRequest getIdentityRequest() {

		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		boolean includeIdentity;
		boolean includePhoto;
		if (documentRepository.getIncludeIdentity()) {
			includeIdentity = true;
			includePhoto = true;
		} else {
			includeIdentity = false;
			includePhoto = false;
		}
		LOG.debug("include identity: " + includeIdentity);
		Boolean removeCard = this.configuration.getValue(
				ConfigProperty.SECURITY_REMOVE_CARD, Boolean.class);
		if (null == removeCard) {
			removeCard = false;
		}
		LOG.debug("remove card: " + removeCard);
		return new IdentityRequest(includeIdentity, false, includePhoto, true,
				removeCard);
	}
}
