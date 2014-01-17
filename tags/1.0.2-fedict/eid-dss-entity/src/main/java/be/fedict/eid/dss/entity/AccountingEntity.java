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

package be.fedict.eid.dss.entity;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Accounting entity holding info on eID DSS Usage.
 * <p/>
 * Holds &lt;domain,#requests&gt;.
 */
@Entity
@Table(name = Constants.DATABASE_TABLE_PREFIX + "accounting")
@NamedQueries({
		@NamedQuery(name = AccountingEntity.LIST_ALL, query = "FROM AccountingEntity AS accounting "
				+ "ORDER BY accounting.requests DESC"),
		@NamedQuery(name = AccountingEntity.RESET_ALL, query = "DELETE FROM AccountingEntity") })
public class AccountingEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String LIST_ALL = "dss.accounting.all";
	public static final String RESET_ALL = "dss.accounting.reset.all";

	private String domain;
	private Long requests;

	public AccountingEntity() {
		super();
	}

	public AccountingEntity(String domain) {
		this.domain = domain;
		this.requests = 1L;
	}

	@Id
	public String getDomain() {
		return domain;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public Long getRequests() {
		return requests;
	}

	public void setRequests(Long requests) {
		this.requests = requests;
	}

	@SuppressWarnings("unchecked")
	public static List<AccountingEntity> listAll(EntityManager entityManager) {

		return entityManager.createNamedQuery(AccountingEntity.LIST_ALL)
				.getResultList();
	}

	public static int resetAll(EntityManager entityManager) {

		return entityManager.createNamedQuery(AccountingEntity.RESET_ALL)
				.executeUpdate();
	}
}
