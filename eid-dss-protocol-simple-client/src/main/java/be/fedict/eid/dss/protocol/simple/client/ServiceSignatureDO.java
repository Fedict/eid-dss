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

package be.fedict.eid.dss.protocol.simple.client;

import java.io.Serializable;
import java.util.List;

/**
 * Signature Request Service Signature DO Object.
 * 
 * @author Wim Vandenhaute
 */
public class ServiceSignatureDO implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String serviceSigned;
	private final String serviceSignature;
	private final String serviceCertificateChainSize;
	private final List<String> serviceCertificates;

	public ServiceSignatureDO(String serviceSigned, String serviceSignature,
			String serviceCertificateChainSize, List<String> serviceCertificates) {

		this.serviceSigned = serviceSigned;
		this.serviceSignature = serviceSignature;
		this.serviceCertificateChainSize = serviceCertificateChainSize;
		this.serviceCertificates = serviceCertificates;
	}

	/**
	 * @return contains the element that are used in the
	 *         {@link #getServiceSignature()}.
	 */
	public String getServiceSigned() {
		return serviceSigned;
	}

	/**
	 * @return base64 encoded signature value.
	 */
	public String getServiceSignature() {
		return serviceSignature;
	}

	/**
	 * @return certificate chain size of the SP Identity.
	 */
	public String getServiceCertificateChainSize() {
		return serviceCertificateChainSize;
	}

	/**
	 * @return base64 encoded certificate chain of the SP Identity.
	 */
	public List<String> getServiceCertificates() {
		return serviceCertificates;
	}
}
