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

package be.fedict.eid.dss.sp;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.sp.bean.SignatureRequestServiceBean;

public class StartupServletContextListener implements ServletContextListener {

	private static final Log LOG = LogFactory
			.getLog(StartupServletContextListener.class);

	private static final String SIGNATURE_REQUEST_BEAN_JNDI = "be/fedict/eid/dss/sp/bean/SignatureRequestServiceBean";

	@Override
	public void contextInitialized(ServletContextEvent sce) {

		try {
			bindComponent(SIGNATURE_REQUEST_BEAN_JNDI,
					new SignatureRequestServiceBean());
		} catch (NamingException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent sce) {
	}

	public static void bindComponent(String jndiName, Object component)
			throws NamingException {

		LOG.debug("bind component: " + jndiName);
		InitialContext initialContext = new InitialContext();
		String[] names = jndiName.split("/");
		Context context = initialContext;
		for (int idx = 0; idx < names.length - 1; idx++) {
			String name = names[idx];
			LOG.debug("name: " + name);
			NamingEnumeration<NameClassPair> listContent = context.list("");
			boolean subContextPresent = false;
			while (listContent.hasMore()) {
				NameClassPair nameClassPair = listContent.next();
				if (!name.equals(nameClassPair.getName())) {
					continue;
				}
				subContextPresent = true;
			}
			if (!subContextPresent) {
				context = context.createSubcontext(name);
			} else {
				context = (Context) context.lookup(name);
			}
		}
		String name = names[names.length - 1];
		context.rebind(name, component);
	}
}
