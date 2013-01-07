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

package be.fedict.eid.dss.webapp;

import java.security.KeyStore.PrivateKeyEntry;

import be.fedict.eid.dss.model.IdentityService;
import be.fedict.eid.dss.spi.DSSProtocolContext;

/**
 * Implementation of the DSS Protocol Context.
 * 
 * @author Frank Cornelis
 */
public class DSSProtocolContextImpl implements DSSProtocolContext {

	private static final long serialVersionUID = 1L;

	private final IdentityService identityService;

	public DSSProtocolContextImpl(IdentityService identityService) {

		this.identityService = identityService;
	}

	public PrivateKeyEntry getIdentity() {
		return this.identityService.findIdentity();
	}
}
