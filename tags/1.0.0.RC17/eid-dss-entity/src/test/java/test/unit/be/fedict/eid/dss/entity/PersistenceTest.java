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

package test.unit.be.fedict.eid.dss.entity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ejb.Ejb3Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import be.fedict.eid.dss.entity.AccountingEntity;
import be.fedict.eid.dss.entity.AdministratorEntity;
import be.fedict.eid.dss.entity.ConfigPropertyEntity;
import be.fedict.eid.dss.entity.DocumentEntity;
import be.fedict.eid.dss.entity.RPEntity;

public class PersistenceTest {

	private static final Log LOG = LogFactory.getLog(PersistenceTest.class);

	private EntityManager entityManager;

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

		configuration.addAnnotatedClass(AdministratorEntity.class);
		configuration.addAnnotatedClass(ConfigPropertyEntity.class);
		configuration.addAnnotatedClass(DocumentEntity.class);
		configuration.addAnnotatedClass(RPEntity.class);
		configuration.addAnnotatedClass(AccountingEntity.class);

		EntityManagerFactory entityManagerFactory = configuration
				.buildEntityManagerFactory();

		this.entityManager = entityManagerFactory.createEntityManager();
		this.entityManager.getTransaction().begin();
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
	public void testHasAdminsQuery() throws Exception {
		assertFalse(AdministratorEntity.hasAdmins(this.entityManager));

		AdministratorEntity administratorEntity = new AdministratorEntity(
				"foobar", "Mr. foobar", true);
		this.entityManager.persist(administratorEntity);

		assertTrue(AdministratorEntity.hasAdmins(this.entityManager));
	}
}
