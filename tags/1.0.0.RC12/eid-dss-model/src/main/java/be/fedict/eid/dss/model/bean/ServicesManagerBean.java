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

package be.fedict.eid.dss.model.bean;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;
import be.fedict.eid.dss.model.DocumentRepository;
import be.fedict.eid.dss.model.ServicesManager;
import be.fedict.eid.dss.spi.DSSDocumentService;
import be.fedict.eid.dss.spi.protocol.DigitalSignatureServiceProtocolType;

/**
 * Services manager EJB3 bean.
 * 
 * @author Frank Cornelis
 */
@Stateless
public class ServicesManagerBean implements ServicesManager {

	private static final Log LOG = LogFactory.getLog(ServicesManagerBean.class);

	@EJB
	private ServicesManagerSingletonBean servicesManagerSingleton;

	public Map<String, String> getProtocolServiceClassNames() {
		return this.servicesManagerSingleton.getProtocolServiceClassNames();
	}

	public List<DigitalSignatureServiceProtocolType> getProtocolServices() {
		return this.servicesManagerSingleton.getProtocolServices();
	}

	public Map<String, String> getDocumentServiceClassNames() {
		return this.servicesManagerSingleton.getDocumentServiceClassNames();
	}

	public DSSDocumentService getDocumentService() {
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		String contentType = documentRepository.getDocumentContentType();
		LOG.debug("content type: " + contentType);
		return this.servicesManagerSingleton.getDocumentService(contentType);
	}

	public DSSDocumentService getDocumentService(String contentType) {
		return this.servicesManagerSingleton.getDocumentService(contentType);
	}

	public List<String> getSupportedDocumentFormats() {
		Set<String> documentFormats = this.servicesManagerSingleton
				.getSupportedDocumentFormats();
		return Collections.list(Collections.enumeration(documentFormats));
	}
}
