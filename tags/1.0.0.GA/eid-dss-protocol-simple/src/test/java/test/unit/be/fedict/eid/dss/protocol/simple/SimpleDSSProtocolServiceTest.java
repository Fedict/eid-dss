/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009-2012 FedICT.
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

package test.unit.be.fedict.eid.dss.protocol.simple;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.Security;
import java.security.cert.X509Certificate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.junit.BeforeClass;
import org.junit.Test;

public class SimpleDSSProtocolServiceTest {

	private static final Log LOG = LogFactory
			.getLog(SimpleDSSProtocolServiceTest.class);

	@BeforeClass
	public static void beforeClass() throws Exception {
		Security.addProvider(new BouncyCastleProvider());
	}

	@Test
	public void testReadOldCertificate() throws Exception {
		InputStream certInputStream = SimpleDSSProtocolServiceTest.class
				.getResourceAsStream("/old-cert.pem");
		InputStreamReader reader = new InputStreamReader(certInputStream);
		PEMReader pemReader = new PEMReader(reader);

		Object object = pemReader.readObject();
		LOG.debug("object type: " + object.getClass().getName());
		X509Certificate certificate = (X509Certificate) object;
		LOG.debug("certificate: " + certificate);
	}

	@Test
	public void testReadNewCertificate() throws Exception {
		InputStream certInputStream = SimpleDSSProtocolServiceTest.class
				.getResourceAsStream("/new-cert.pem");
		InputStreamReader reader = new InputStreamReader(certInputStream);
		PEMReader pemReader = new PEMReader(reader);

		Object object = pemReader.readObject();
		LOG.debug("object type: " + object.getClass().getName());
		X509Certificate certificate = (X509Certificate) object;
		LOG.debug("certificate: " + certificate);
	}
}
