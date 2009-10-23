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

package be.fedict.eid.dss.portal.model;

import java.io.StringWriter;
import java.math.BigInteger;

import javax.ejb.Remove;
import javax.ejb.Stateful;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.bouncycastle.util.encoders.Base64;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.contexts.SessionContext;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;

import be.fedict.eid.applet.service.Identity;
import be.fedict.eid.dss.demo.cert.request._1.CertificateRequestType;
import be.fedict.eid.dss.demo.cert.request._1.CertificateTypeType;
import be.fedict.eid.dss.demo.cert.request._1.EntityType;
import be.fedict.eid.dss.demo.cert.request._1.ObjectFactory;

@Stateful
@Name("csrForm")
@LocalBinding(jndiBinding = "fedict/eid/dss/portal/CSRFormBean")
public class CSRFormBean implements CSRForm {

	@Logger
	private Log log;

	private String csr;

	private String description;

	private String type;

	private String dn;

	private String operatorFunction;

	private String operatorPhone;

	private String operatorEmail;

	private String validityPeriod;

	private String signatureRequest;

	@In
	private SessionContext sessionContext;

	@In
	private FacesMessages facesMessages;

	@Remove
	@Destroy
	public void destroy() {
		this.log.debug("destroy");
	}

	public String getCsr() {
		return this.csr;
	}

	public String getDescription() {
		return this.description;
	}

	public String getDn() {
		return this.dn;
	}

	public String getOperatorEmail() {
		return this.operatorEmail;
	}

	public String getOperatorFunction() {
		return this.operatorFunction;
	}

	public String getOperatorPhone() {
		return this.operatorPhone;
	}

	public String getType() {
		return this.type;
	}

	public void setCsr(String csr) {
		this.csr = csr;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setDn(String dn) {
		this.dn = dn;
	}

	public void setOperatorEmail(String operatorEmail) {
		this.operatorEmail = operatorEmail;
	}

	public void setOperatorFunction(String operatorFunction) {
		this.operatorFunction = operatorFunction;
	}

	public void setOperatorPhone(String operatorPhone) {
		this.operatorPhone = operatorPhone;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String submit() {
		this.log.debug("submit");

		ObjectFactory objectFactory = new ObjectFactory();
		CertificateRequestType certificateRequest = objectFactory
				.createCertificateRequestType();
		certificateRequest.setDistinguishedName(this.dn);
		CertificateTypeType certificateType = CertificateTypeType
				.fromValue(this.type);
		certificateRequest.setCertificateType(certificateType);
		certificateRequest
				.setValidityPeriod(new BigInteger(this.validityPeriod));
		certificateRequest.setCSR(this.csr);
		certificateRequest.setDescription(this.description);

		EntityType technicalOperator = objectFactory.createEntityType();
		certificateRequest.setTechnicalOperator(technicalOperator);
		Identity operatorIdentity = (Identity) this.sessionContext
				.get("eid.identity");
		technicalOperator.setName(operatorIdentity.name + " "
				+ operatorIdentity.firstName);
		technicalOperator.setFunction(this.operatorFunction);
		technicalOperator.setEmail(this.operatorEmail);
		technicalOperator.setPhone(this.operatorPhone);

		try {
			JAXBContext jaxbContext = JAXBContext
					.newInstance(ObjectFactory.class);
			Marshaller marshaller = jaxbContext.createMarshaller();
			StringWriter stringWriter = new StringWriter();
			marshaller
					.marshal(objectFactory
							.createCertificateRequest(certificateRequest),
							stringWriter);
			this.log.debug("document: " + stringWriter.toString());
			this.signatureRequest = new String(Base64.encode(stringWriter
					.toString().getBytes()));
		} catch (JAXBException e) {
			this.log.debug("JAXB error: " + e.getMessage(), e);
			return null;
		}

		return "success";
	}

	public String getValidityPeriod() {
		return this.validityPeriod;
	}

	public void setValidityPeriod(String validityPeriod) {
		this.validityPeriod = validityPeriod;
	}

	public String getSignatureRequest() {
		return this.signatureRequest;
	}

	public void setSignatureRequest(String signatureRequest) {
		this.signatureRequest = signatureRequest;
	}
}
