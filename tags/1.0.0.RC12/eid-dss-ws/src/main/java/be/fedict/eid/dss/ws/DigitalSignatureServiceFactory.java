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

package be.fedict.eid.dss.ws;

import java.net.URL;

import javax.xml.namespace.QName;

public class DigitalSignatureServiceFactory {

	public static final String WSDL_RESOURCE = "/eid-dss-ws.wsdl";

	public DigitalSignatureServiceFactory() {
		super();
	}

	public static DigitalSignatureService getInstance() {
		URL wsdlLocation = DigitalSignatureServiceFactory.class
				.getResource(WSDL_RESOURCE);
		if (null == wsdlLocation) {
			throw new RuntimeException("WSDL location not valid: "
					+ WSDL_RESOURCE);
		}
		QName serviceName = new QName("urn:be:fedict:eid:dss:ws",
				"DigitalSignatureService");
		DigitalSignatureService service = new DigitalSignatureService(
				wsdlLocation, serviceName);
		return service;
	}
}
