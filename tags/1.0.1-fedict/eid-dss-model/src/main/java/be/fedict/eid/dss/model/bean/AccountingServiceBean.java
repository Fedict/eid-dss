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

package be.fedict.eid.dss.model.bean;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.entity.AccountingEntity;
import be.fedict.eid.dss.model.AccountingService;

@Stateless
public class AccountingServiceBean implements AccountingService {

	private static Log LOG = LogFactory.getLog(AccountingServiceBean.class);

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@inheritDoc}
	 */
	public List<AccountingEntity> listAll() {

		LOG.debug("list all");
		return AccountingEntity.listAll(this.entityManager);
	}

	/**
	 * {@inheritDoc}
	 */
	public void resetAll() {

		LOG.debug("reset all: #deleted="
				+ AccountingEntity.resetAll(this.entityManager));
	}

	/**
	 * {@inheritDoc}
	 */
	public AccountingEntity addRequest(String domain) {

		LOG.debug("Add request: " + domain);

		String domainKey;
		if (domain.indexOf("?") != -1) {
			domainKey = domain.substring(0, domain.indexOf("?"));
		} else {
			domainKey = domain;
		}

		AccountingEntity accountingEntity = this.entityManager.find(
				AccountingEntity.class, domainKey);
		if (null == accountingEntity) {

			accountingEntity = new AccountingEntity(domainKey);
			this.entityManager.persist(accountingEntity);

		} else {

			accountingEntity.setRequests(accountingEntity.getRequests() + 1);

		}
		return accountingEntity;
	}
}
