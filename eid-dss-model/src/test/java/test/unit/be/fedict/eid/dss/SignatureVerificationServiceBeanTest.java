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

package test.unit.be.fedict.eid.dss;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

public class SignatureVerificationServiceBeanTest {

	private static final Log LOG = LogFactory
			.getLog(SignatureVerificationServiceBeanTest.class);

	@Test
	public void testExtractSerialNumberFromDN() throws Exception {
		String dn = "SERIALNUMBER=71715100070, GIVENNAME=Alice Geldigekaart2266, SURNAME=SPECIMEN, CN=Alice SPECIMEN (Authentication), C=BE";
		X500Principal principal = new X500Principal(dn);
		LOG.debug("principal: " + principal);
	}
}
