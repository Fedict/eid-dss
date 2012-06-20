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

package be.fedict.eid.dss.entity;

import java.io.Serializable;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

@Entity
@Table(name = Constants.DATABASE_TABLE_PREFIX + "config")
@NamedQueries({
		@NamedQuery(name = ConfigPropertyEntity.LIST_INDEXES, query = "FROM ConfigPropertyEntity WHERE name LIKE :name"),
		@NamedQuery(name = ConfigPropertyEntity.LIST_ALL, query = "FROM ConfigPropertyEntity") })
public class ConfigPropertyEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String LIST_INDEXES = "dss.config.list.idx";
	public static final String LIST_ALL = "dss.config.list.all";

	private String name;

	private String value;

	public ConfigPropertyEntity() {
		super();
	}

	public ConfigPropertyEntity(String name, String value) {
		this.name = name;
		this.value = value;
	}

	@Id
	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@SuppressWarnings("unchecked")
	public static List<ConfigPropertyEntity> listConfigsWhereNameLike(
			EntityManager entityManager, String name) {

		return entityManager
				.createNamedQuery(ConfigPropertyEntity.LIST_INDEXES)
				.setParameter("name", "%" + name + "%").getResultList();
	}

	@SuppressWarnings("unchecked")
	public static List<ConfigPropertyEntity> listAll(EntityManager entityManager) {

		return entityManager.createNamedQuery(ConfigPropertyEntity.LIST_ALL)
				.getResultList();
	}
}
