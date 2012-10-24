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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Query;
import javax.persistence.Table;

@Entity
@Table(name = Constants.DATABASE_TABLE_PREFIX + "xml_schemas")
@NamedQueries({ @NamedQuery(name = XmlSchemaEntity.ALL, query = "FROM XmlSchemaEntity") })
public class XmlSchemaEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String ALL = "dss.xmlschema.all";

	private String namespace;

	private String revision;

	private byte[] xsd;

	public XmlSchemaEntity() {
		super();
	}

	public XmlSchemaEntity(String namespace, String revision, byte[] xsd) {
		this.namespace = namespace;
		this.revision = revision;
		this.xsd = xsd;
	}

	@Id
	public String getNamespace() {
		return this.namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getRevision() {
		return this.revision;
	}

	public void setRevision(String revision) {
		this.revision = revision;
	}

	@Column(length = 100 * 1024)
	public byte[] getXsd() {
		return this.xsd;
	}

	public void setXsd(byte[] xsd) {
		this.xsd = xsd;
	}

	@SuppressWarnings("unchecked")
	public static List<XmlSchemaEntity> getAll(EntityManager entityManager) {
		Query query = entityManager.createNamedQuery(ALL);
		return query.getResultList();
	}
}
