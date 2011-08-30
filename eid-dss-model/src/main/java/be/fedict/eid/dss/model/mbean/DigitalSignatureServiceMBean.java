/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010-2011 FedICT.
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

package be.fedict.eid.dss.model.mbean;

import java.security.Provider;
import java.security.Security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.jboss.ejb3.annotation.Service;

/**
 * MBean to manage some runtime aspects like registering security providers.
 * 
 * @author Frank Cornelis
 * 
 */
@Service
public class DigitalSignatureServiceMBean implements
		DigitalSignatureServiceMBeanLocal {

	private static final Log LOG = LogFactory
			.getLog(DigitalSignatureServiceMBean.class);

	private Provider managedProvider;

	public void create() throws Exception {
		LOG.debug("create");
	}

	public void start() throws Exception {
		LOG.debug("start");
		Provider provider = Security
				.getProvider(BouncyCastleProvider.PROVIDER_NAME);
		if (null != provider) {
			LOG.debug("we don't register BouncyCastle");
			return;
		}
		this.managedProvider = new BouncyCastleProvider();
		LOG.debug("we register BouncyCastle");
		if (-1 == Security.addProvider(this.managedProvider)) {
			LOG.fatal("could not register BouncyCastle");
		}
	}

	public void stop() {
		LOG.debug("stop");
		if (null == this.managedProvider) {
			LOG.debug("we don't unregister BouncyCastle");
			return;
		}
		LOG.debug("we unregister BouncyCastle");
		Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
	}

	public void destroy() {
		LOG.debug("destroy");
	}
}
