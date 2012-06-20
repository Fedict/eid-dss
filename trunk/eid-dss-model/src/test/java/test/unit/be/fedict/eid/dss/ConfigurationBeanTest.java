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

package test.unit.be.fedict.eid.dss;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Field;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ejb.Ejb3Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import be.fedict.eid.dss.entity.ConfigPropertyEntity;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.TSPDigestAlgo;
import be.fedict.eid.dss.model.bean.ConfigurationBean;

public class ConfigurationBeanTest {

	private static final Log LOG = LogFactory
			.getLog(ConfigurationBeanTest.class);

	private EntityManager entityManager;

	private ConfigurationBean testedInstance;

	@Before
	public void setUp() throws Exception {
		Class.forName("org.hsqldb.jdbcDriver");
		Ejb3Configuration configuration = new Ejb3Configuration();
		configuration.setProperty("hibernate.dialect",
				"org.hibernate.dialect.HSQLDialect");
		configuration.setProperty("hibernate.connection.driver_class",
				"org.hsqldb.jdbcDriver");
		configuration.setProperty("hibernate.connection.url",
				"jdbc:hsqldb:mem:beta");
		configuration.setProperty("hibernate.hbm2ddl.auto", "create");

		configuration.addAnnotatedClass(ConfigPropertyEntity.class);

		EntityManagerFactory entityManagerFactory = configuration
				.buildEntityManagerFactory();

		this.entityManager = entityManagerFactory.createEntityManager();
		this.entityManager.getTransaction().begin();

		this.testedInstance = new ConfigurationBean();
		Field[] beanFields = ConfigurationBean.class.getDeclaredFields();
		for (Field beanField : beanFields) {
			if (null == beanField.getAnnotation(PersistenceContext.class)) {
				continue;
			}
			beanField.setAccessible(true);
			beanField.set(this.testedInstance, this.entityManager);
		}
	}

	@After
	public void tearDown() throws Exception {
		EntityTransaction entityTransaction = this.entityManager
				.getTransaction();
		LOG.debug("entity manager open: " + this.entityManager.isOpen());
		LOG.debug("entity transaction active: " + entityTransaction.isActive());
		if (entityTransaction.isActive()) {
			if (entityTransaction.getRollbackOnly()) {
				entityTransaction.rollback();
			} else {
				entityTransaction.commit();
			}
		}
		this.entityManager.close();
	}

	@Test
	public void testEnum() throws Exception {
		assertNull(this.testedInstance.getValue(ConfigProperty.TSP_DIGEST_ALGO,
				TSPDigestAlgo.class));

		this.testedInstance.setValue(ConfigProperty.TSP_DIGEST_ALGO,
				TSPDigestAlgo.SHA256);

		assertEquals(TSPDigestAlgo.SHA256, this.testedInstance.getValue(
				ConfigProperty.TSP_DIGEST_ALGO, TSPDigestAlgo.class));
	}

	@Test
	public void testString() throws Exception {
		assertNull(this.testedInstance.getValue(ConfigProperty.TSP_URL,
				String.class));

		this.testedInstance.setValue(ConfigProperty.TSP_URL, "foobar");

		assertEquals("foobar", this.testedInstance.getValue(
				ConfigProperty.TSP_URL, String.class));
	}
}
