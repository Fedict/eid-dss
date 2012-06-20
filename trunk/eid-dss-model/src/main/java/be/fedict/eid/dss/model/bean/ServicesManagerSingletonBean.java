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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.TrustValidationService;
import be.fedict.eid.dss.model.XmlSchemaManager;
import be.fedict.eid.dss.model.XmlStyleSheetManager;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.DSSDocumentService;
import be.fedict.eid.dss.spi.document.DigitalSignatureServiceDocumentType;
import be.fedict.eid.dss.spi.protocol.DigitalSignatureServiceProtocolType;
import be.fedict.eid.dss.spi.protocol.ObjectFactory;

/**
 * EJB 3.1 singleton services manager bean.
 * 
 * @author Frank Cornelis
 */
@Singleton
@Startup
public class ServicesManagerSingletonBean {

	private static final Log LOG = LogFactory.getLog(ServicesManagerBean.class);

	private Enumeration<URL> getResources(String resourceName)
			throws IOException {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		return classLoader.getResources(resourceName);
	}

	private Map<String, String> protocolServiceClassNames;

	private Map<String, String> documentServiceClassNames;

	@EJB
	private XmlSchemaManager xmlSchemaManager;

	@EJB
	private XmlStyleSheetManager xmlStyleSheetManager;

	@EJB
	private TrustValidationService trustValidationService;

	@EJB
	private Configuration configuration;

	@PostConstruct
	public void postConstruct() {
		LOG.debug("post construct");
		this.protocolServiceClassNames = loadProtocolServiceClassNames();
		this.documentServiceClassNames = loadDocumentServiceClassNames();
	}

	public Map<String, String> getProtocolServiceClassNames() {
		return this.protocolServiceClassNames;
	}

	private Map<String, String> loadProtocolServiceClassNames() {

		LOG.debug("load protocol service class names");
		List<DigitalSignatureServiceProtocolType> protocolServices = getProtocolServices();

		Map<String, String> protocolServiceClassNames = new HashMap<String, String>();

		for (DigitalSignatureServiceProtocolType protocolService : protocolServices) {
			protocolServiceClassNames.put(protocolService.getContextPath(),
					protocolService.getProtocolServiceClass());
		}
		return protocolServiceClassNames;
	}

	@SuppressWarnings("unchecked")
	public List<DigitalSignatureServiceProtocolType> getProtocolServices() {

		LOG.debug("getProtocolServices");

		List<DigitalSignatureServiceProtocolType> protocolServices = new LinkedList<DigitalSignatureServiceProtocolType>();
		Enumeration<URL> resources;
		try {
			resources = getResources("META-INF/eid-dss-protocol.xml");
		} catch (IOException e) {
			LOG.error("I/O error: " + e.getMessage(), e);
			return protocolServices;
		}
		Unmarshaller unmarshaller;
		try {
			JAXBContext jaxbContext = JAXBContext
					.newInstance(ObjectFactory.class);
			unmarshaller = jaxbContext.createUnmarshaller();
		} catch (JAXBException e) {
			LOG.error("JAXB error: " + e.getMessage(), e);
			return protocolServices;
		}
		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			LOG.debug("resource URL: " + resource.toString());
			JAXBElement<DigitalSignatureServiceProtocolType> jaxbElement;
			try {
				jaxbElement = (JAXBElement<DigitalSignatureServiceProtocolType>) unmarshaller
						.unmarshal(resource);
			} catch (JAXBException e) {
				LOG.error("JAXB error: " + e.getMessage(), e);
				continue;
			}

			protocolServices.add(jaxbElement.getValue());
		}
		return protocolServices;

	}

	public Map<String, String> getDocumentServiceClassNames() {
		return this.documentServiceClassNames;
	}

	public Set<String> getSupportedDocumentFormats() {
		return this.documentServiceClassNames.keySet();
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> loadDocumentServiceClassNames() {
		LOG.debug("getDocumentServiceClassNames");
		Map<String, String> documentServiceClassNames = new HashMap<String, String>();
		Enumeration<URL> resources;
		try {
			resources = getResources("META-INF/eid-dss-document.xml");
		} catch (IOException e) {
			LOG.error("I/O error: " + e.getMessage(), e);
			return documentServiceClassNames;
		}
		Unmarshaller unmarshaller;
		try {
			JAXBContext jaxbContext = JAXBContext
					.newInstance(be.fedict.eid.dss.spi.document.ObjectFactory.class);
			unmarshaller = jaxbContext.createUnmarshaller();
		} catch (JAXBException e) {
			LOG.error("JAXB error: " + e.getMessage(), e);
			return documentServiceClassNames;
		}
		while (resources.hasMoreElements()) {
			URL resource = resources.nextElement();
			LOG.debug("resource URL: " + resource.toString());
			JAXBElement<DigitalSignatureServiceDocumentType> jaxbElement;
			try {
				jaxbElement = (JAXBElement<DigitalSignatureServiceDocumentType>) unmarshaller
						.unmarshal(resource);
			} catch (JAXBException e) {
				LOG.error("JAXB error: " + e.getMessage(), e);
				continue;
			}
			DigitalSignatureServiceDocumentType digitalSignatureServiceDocument = jaxbElement
					.getValue();
			String documentServiceClassName = digitalSignatureServiceDocument
					.getDocumentServiceClass();
			List<String> contentTypes = digitalSignatureServiceDocument
					.getContentType();
			for (String contentType : contentTypes) {
				documentServiceClassNames.put(contentType,
						documentServiceClassName);
			}
		}
		return documentServiceClassNames;
	}

	@SuppressWarnings("unchecked")
	public DSSDocumentService getDocumentService(String contentType) {
		LOG.debug("getDocumentService");
		String documentServiceClassName = this.documentServiceClassNames
				.get(contentType);
		if (null == documentServiceClassName) {
			throw new IllegalArgumentException("unsupported content type: "
					+ contentType);
		}
		Class<? extends DSSDocumentService> documentServiceClass;
		try {
			documentServiceClass = (Class<? extends DSSDocumentService>) Class
					.forName(documentServiceClassName);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("document service class not found: "
					+ documentServiceClassName, e);
		}
		DSSDocumentService documentService;
		try {
			documentService = documentServiceClass.newInstance();
		} catch (Exception e) {
			throw new RuntimeException(
					"could not create instance of document service: "
							+ documentServiceClassName, e);
		}
		DSSDocumentContext documentContext = new ModelDSSDocumentContext(
				this.xmlSchemaManager, this.xmlStyleSheetManager,
				this.trustValidationService, this.configuration);
		try {
			documentService.init(documentContext, contentType);
		} catch (Exception e) {
			throw new RuntimeException(
					"error initializing the document service: "
							+ e.getMessage(), e);
		}
		return documentService;
	}
}
