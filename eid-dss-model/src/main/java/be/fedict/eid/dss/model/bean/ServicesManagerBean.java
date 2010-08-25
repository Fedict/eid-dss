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
import java.util.Map;

import javax.ejb.Stateless;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.model.ServicesManager;
import be.fedict.eid.dss.spi.document.DigitalSignatureServiceDocumentType;
import be.fedict.eid.dss.spi.protocol.DigitalSignatureServiceProtocolType;
import be.fedict.eid.dss.spi.protocol.ObjectFactory;

@Stateless
public class ServicesManagerBean implements ServicesManager {

	private static final Log LOG = LogFactory.getLog(ServicesManagerBean.class);

	private Enumeration<URL> getResources(String resourceName)
			throws IOException {
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		Enumeration<URL> resources = classLoader.getResources(resourceName);
		return resources;
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getProtocolServiceClassNames() {
		LOG.debug("getProtocolServices");
		Map<String, String> protocolServiceClassNames = new HashMap<String, String>();
		Enumeration<URL> resources;
		try {
			resources = getResources("META-INF/eid-dss-protocol.xml");
		} catch (IOException e) {
			LOG.error("I/O error: " + e.getMessage(), e);
			return protocolServiceClassNames;
		}
		Unmarshaller unmarshaller;
		try {
			JAXBContext jaxbContext = JAXBContext
					.newInstance(ObjectFactory.class);
			unmarshaller = jaxbContext.createUnmarshaller();
		} catch (JAXBException e) {
			LOG.error("JAXB error: " + e.getMessage(), e);
			return protocolServiceClassNames;
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
			DigitalSignatureServiceProtocolType dssProtocol = jaxbElement
					.getValue();
			String contextPath = dssProtocol.getContextPath();
			String protocolServiceClassName = dssProtocol
					.getProtocolServiceClass();
			protocolServiceClassNames
					.put(contextPath, protocolServiceClassName);
		}
		return protocolServiceClassNames;
	}

	@SuppressWarnings("unchecked")
	public Map<String, String> getDocumentServiceClassNames() {
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
			String contentType = digitalSignatureServiceDocument
					.getContentType();
			String documentServiceClassName = digitalSignatureServiceDocument
					.getDocumentServiceClass();
			documentServiceClassNames
					.put(contentType, documentServiceClassName);
		}
		return documentServiceClassNames;
	}
}
