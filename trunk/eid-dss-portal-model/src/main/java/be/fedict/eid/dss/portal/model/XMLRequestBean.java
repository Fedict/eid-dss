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

import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.bouncycastle.util.encoders.Base64;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;

@Stateful
@Name("xmlRequest")
@LocalBinding(jndiBinding = "fedict/eid/dss/portal/XMLRequestBean")
public class XMLRequestBean implements XMLRequest {

	@Logger
	private Log log;

	private String encodedDocument;

	private String document;

	@Remove
	@Destroy
	public void destroy() {
		this.log.debug("destroy");
	}

	public String getDocument() {
		return this.document;
	}

	public void setDocument(String document) {
		this.document = document;
	}

	public String getEncodedDocument() {
		return this.encodedDocument;
	}

	public void setEncodedDocument(String encodedDocument) {
		this.encodedDocument = encodedDocument;
	}

	public String submit() {
		this.log.debug("submit");
		this.encodedDocument = new String(Base64.encode(this.document
				.getBytes()));
		return "success";
	}
}
