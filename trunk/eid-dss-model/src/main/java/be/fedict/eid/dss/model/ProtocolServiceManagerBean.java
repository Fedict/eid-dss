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

package be.fedict.eid.dss.model;

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

import be.fedict.eid.dss.spi.protocol.DigitalSignatureServiceProtocolType;
import be.fedict.eid.dss.spi.protocol.ObjectFactory;

@Stateless
public class ProtocolServiceManagerBean implements ProtocolServiceManager {

	private static final Log LOG = LogFactory
			.getLog(ProtocolServiceManagerBean.class);

	@SuppressWarnings("unchecked")
	public Map<String, String> getProtocolServices() {
		LOG.debug("getProtocolServices");
		Map<String, String> protocolServices = new HashMap<String, String>();
		ClassLoader classLoader = Thread.currentThread()
				.getContextClassLoader();
		Enumeration<URL> resources;
		try {
			resources = classLoader
					.getResources("META-INF/eid-idp-protocol.xml");
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
			DigitalSignatureServiceProtocolType dssProtocol = jaxbElement
					.getValue();
			String contextPath = dssProtocol.getContextPath();
			String protocolServiceClass = dssProtocol.getProtocolServiceClass();
			protocolServices.put(contextPath, protocolServiceClass);
		}
		return protocolServices;
	}
}
