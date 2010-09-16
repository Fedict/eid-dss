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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import javax.ejb.EJB;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.ocsp.OCSPResp;
import org.easymock.EasyMock;
import org.junit.Test;

import be.fedict.eid.dss.model.SignatureInfo;
import be.fedict.eid.dss.model.TrustValidationService;
import be.fedict.eid.dss.model.bean.SignatureVerificationServiceBean;

public class SignatureVerificationServiceBeanTest {

	private static final Log LOG = LogFactory
			.getLog(SignatureVerificationServiceBeanTest.class);

	@Test
	public void testExtractSerialNumberFromDN() throws Exception {
		String dn = "SERIALNUMBER=71715100070, GIVENNAME=Alice Geldigekaart2266, SURNAME=SPECIMEN, CN=Alice SPECIMEN (Authentication), C=BE";
		X500Principal principal = new X500Principal(dn);
		LOG.debug("principal: " + principal);
	}

	@Test
	public void testVerifySignedDocument() throws Exception {
		// setup
		InputStream signedDocumentInputStream = SignatureVerificationServiceBeanTest.class
				.getResourceAsStream("/signed-document.xml");
		byte[] signedDocument = IOUtils.toByteArray(signedDocumentInputStream);
		SignatureVerificationServiceBean testedInstance = new SignatureVerificationServiceBean();

		TrustValidationService mockTrustValidationService = EasyMock
				.createMock(TrustValidationService.class);
		inject(testedInstance, mockTrustValidationService);
		testedInstance.postConstruct();

		// expectations
		mockTrustValidationService.validate(
				(List<X509Certificate>) EasyMock.anyObject(),
				(Date) EasyMock.anyObject(),
				(List<OCSPResp>) EasyMock.anyObject(),
				(List<X509CRL>) EasyMock.anyObject());

		// prepare
		EasyMock.replay(mockTrustValidationService);

		// operate
		List<SignatureInfo> result = testedInstance.verify(signedDocument);

		// verify
		EasyMock.verify(mockTrustValidationService);
		assertNotNull(result);
		assertEquals(1, result.size());
		SignatureInfo signatureInfo = result.get(0);
		assertNotNull(signatureInfo.getSigner());
		LOG.debug("signer: "
				+ signatureInfo.getSigner().getSubjectX500Principal());
		assertTrue(signatureInfo.getSigner().getSubjectX500Principal()
				.toString().contains("Frank Cornelis"));
		assertNotNull(signatureInfo.getSigningTime());
		LOG.debug("signing time: " + signatureInfo.getSigningTime());
	}

	private void inject(Object bean, Object ejbService)
			throws IllegalArgumentException, IllegalAccessException {
		Class<?> beanClass = bean.getClass();
		Field[] fields = beanClass.getDeclaredFields();
		for (Field field : fields) {
			EJB ejbAnnotation = field.getAnnotation(EJB.class);
			if (null == ejbAnnotation) {
				continue;
			}
			if (field.getType().isAssignableFrom(ejbService.getClass())) {
				field.setAccessible(true);
				field.set(bean, ejbService);
			}
		}
	}
}
