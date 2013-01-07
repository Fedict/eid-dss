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

package be.fedict.eid.dss.control.bean;

import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.log.Log;

import be.fedict.eid.dss.control.ServiceEndpoint;
import be.fedict.eid.dss.control.ServiceInfo;
import be.fedict.eid.dss.model.IdentityService;
import be.fedict.eid.dss.model.ServicesManager;
import be.fedict.eid.dss.model.XmlSchemaManager;
import be.fedict.eid.dss.model.XmlStyleSheetManager;
import be.fedict.eid.dss.spi.protocol.DigitalSignatureServiceProtocolType;

@Stateful
@Name("dssServiceInfo")
@LocalBinding(jndiBinding = "fedict/eid/dss/ServiceInfoBean")
public class ServiceInfoBean implements ServiceInfo {

	@Logger
	private Log log;

	@SuppressWarnings("unused")
	@DataModel
	private List<String> dssDocumentFormatList;

	@DataModel
	private List<ServiceEndpoint> dssProtocolServices;

	@SuppressWarnings("unused")
	@DataModel
	private List<String> dssXmlSchemaNamespaces;

	@SuppressWarnings("unused")
	@DataModel
	private List<String> dssXmlStyleSheetNamespaces;

	@EJB
	private ServicesManager servicesManager;

	@EJB
	private XmlSchemaManager xmlSchemaManager;

	@EJB
	private XmlStyleSheetManager xmlStyleSheetManager;

	@EJB
	private IdentityService identityService;

	@Remove
	@Destroy
	@Override
	public void destroy() {
		this.log.debug("destroy");
	}

	@Override
	@Factory("dssDocumentFormatList")
	public void initDocumentFormatList() {
		this.dssDocumentFormatList = this.servicesManager
				.getSupportedDocumentFormats();
	}

	@Override
	@Factory("dssProtocolServices")
	public void initProtocolServices() {

		this.log.debug("init dssProtocolServices");
		this.dssProtocolServices = new LinkedList<ServiceEndpoint>();

		for (DigitalSignatureServiceProtocolType protocolService : this.servicesManager
				.getProtocolServices()) {
			this.dssProtocolServices.add(new ServiceEndpoint(protocolService
					.getName(), "/eid-dss/protocol"
					+ protocolService.getContextPath()));
		}
	}

	@Override
	@Factory("dssXmlSchemaNamespaces")
	public void initXmlSchemaNamespacesList() {
		this.dssXmlSchemaNamespaces = this.xmlSchemaManager
				.getXmlSchemaNamespaces();
	}

	@Override
	@Factory("dssXmlStyleSheetNamespaces")
	public void initXmlStyleSheetNamespacesList() {
		this.dssXmlStyleSheetNamespaces = this.xmlStyleSheetManager
				.getXmlStyleSheetNamespaces();
	}

	@Override
	@Factory("dssServiceFingerprint")
	public String getServiceFingerprint() {

		String thumbprint = this.identityService.getIdentityFingerprint();
		if (null == thumbprint) {
			return "<No identity configured>";
		}
		return thumbprint;
	}

	@Override
	public String getIdentityCertificateChain() {
		List<X509Certificate> identityCertificateChain = this.identityService
				.getIdentityCertificateChain();
		if (identityCertificateChain.isEmpty()) {
			return "No service identity configured.";
		}
		StringBuffer stringBuffer = new StringBuffer();
		for (X509Certificate cert : identityCertificateChain) {
			stringBuffer.append(cert.toString());
		}
		return stringBuffer.toString();
	}
}
