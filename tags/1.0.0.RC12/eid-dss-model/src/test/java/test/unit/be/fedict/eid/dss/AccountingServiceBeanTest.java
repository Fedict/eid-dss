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

import java.lang.reflect.Field;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.Test;

import be.fedict.eid.dss.entity.AccountingEntity;
import be.fedict.eid.dss.model.AccountingService;
import be.fedict.eid.dss.model.bean.AccountingServiceBean;

public class AccountingServiceBeanTest {

	@Test
	public void testAddRequest() throws Exception {
		AccountingService testedInstance = new AccountingServiceBean();

		EntityManager mockEntityManager = EasyMock
				.createMock(EntityManager.class);
		injectPersistenceContext(mockEntityManager, testedInstance);

		EasyMock.expect(
				mockEntityManager.find(AccountingEntity.class,
						"https://localhost:8080/eid-dss/entry"))
				.andReturn(null);

		Capture<AccountingEntity> accountingEntityCapture = new Capture<AccountingEntity>();
		mockEntityManager.persist(EasyMock.capture(accountingEntityCapture));

		EasyMock.replay(mockEntityManager);

		testedInstance
				.addRequest("https://localhost:8080/eid-dss/entry?param=1234?param2=5678");

		EasyMock.verify(mockEntityManager);

		AccountingEntity accountingEntity = accountingEntityCapture.getValue();
		assertEquals("https://localhost:8080/eid-dss/entry",
				accountingEntity.getDomain());
		assertEquals(new Long(1), accountingEntity.getRequests());
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
