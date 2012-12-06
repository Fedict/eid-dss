/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010 Frank Cornelis.
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

import java.io.InputStream;
import java.lang.reflect.Field;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.IOUtils;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import be.fedict.eid.dss.entity.XmlSchemaEntity;
import be.fedict.eid.dss.model.bean.XmlSchemaManagerBean;

public class XmlSchemaManagerBeanTest {

	@Test
	public void testAdd() throws Exception {
		// setup
		XmlSchemaManagerBean testedInstance = new XmlSchemaManagerBean();
		InputStream xsdInputStream = XmlSchemaManagerBeanTest.class
				.getResourceAsStream("/example.xsd");
		EntityManager mockEntityManager = EasyMock
				.createMock(EntityManager.class);
		injectPersistenceContext(mockEntityManager, testedInstance);

		// expectations
		EasyMock.expect(
				mockEntityManager.find(XmlSchemaEntity.class,
						"urn:be:fedict:eid:dss:example")).andReturn(null);

		Capture<XmlSchemaEntity> persistCapture = new Capture<XmlSchemaEntity>();
		mockEntityManager.persist(EasyMock.capture(persistCapture));

		// prepare
		EasyMock.replay(mockEntityManager);

		// operate
		testedInstance.add("1.0", xsdInputStream);

		// verify
		EasyMock.verify(mockEntityManager);
		XmlSchemaEntity resultEntity = persistCapture.getValue();
		assertEquals("urn:be:fedict:eid:dss:example",
				resultEntity.getNamespace());
		assertEquals("1.0", resultEntity.getRevision());
	}

	@Test
	public void testAdd2() throws Exception {
		// setup
		XmlSchemaManagerBean testedInstance = new XmlSchemaManagerBean();
		InputStream xsdInputStream = XmlSchemaManagerBeanTest.class
				.getResourceAsStream("/example.xsd");
		byte[] xsd = IOUtils.toByteArray(xsdInputStream);
		InputStream xsd2InputStream = XmlSchemaManagerBeanTest.class
				.getResourceAsStream("/example2.xsd");
		EntityManager mockEntityManager = EasyMock
				.createMock(EntityManager.class);
		injectPersistenceContext(mockEntityManager, testedInstance);

		XmlSchemaEntity exampleXmlSchemaEntity = new XmlSchemaEntity("", "1.0",
				xsd);

		// expectations
		EasyMock.expect(
				mockEntityManager.find(XmlSchemaEntity.class,
						"urn:be:fedict:eid:dss:example")).andReturn(
				exampleXmlSchemaEntity);
		EasyMock.expect(
				mockEntityManager.find(XmlSchemaEntity.class,
						"urn:be:fedict:eid:dss:example2")).andReturn(null);

		Capture<XmlSchemaEntity> persistCapture = new Capture<XmlSchemaEntity>();
		mockEntityManager.persist(EasyMock.capture(persistCapture));

		// prepare
		EasyMock.replay(mockEntityManager);

		// operate
		testedInstance.add("1.0", xsd2InputStream);

		// verify
		EasyMock.verify(mockEntityManager);
		XmlSchemaEntity resultEntity = persistCapture.getValue();
		assertEquals("urn:be:fedict:eid:dss:example2",
				resultEntity.getNamespace());
		assertEquals("1.0", resultEntity.getRevision());
	}

	private void injectPersistenceContext(EntityManager entityManager,
			Object bean) throws IllegalArgumentException,
			IllegalAccessException {
		Class<?> beanClass = bean.getClass();
		Field[] fields = beanClass.getDeclaredFields();
		for (Field field : fields) {
			PersistenceContext persistenceContextAnnotation = field
					.getAnnotation(PersistenceContext.class);
			if (null == persistenceContextAnnotation) {
				continue;
			}
			field.setAccessible(true);
			field.set(bean, entityManager);
		}
	}
}
