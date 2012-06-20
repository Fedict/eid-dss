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

package be.fedict.eid.dss.entity;

import java.io.Serializable;
import java.util.Date;
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
@Table(name = Constants.DATABASE_TABLE_PREFIX + "documents")
@NamedQueries({
		@NamedQuery(name = DocumentEntity.ALL, query = "FROM DocumentEntity"),
		@NamedQuery(name = DocumentEntity.REMOVE_EXPIRED, query = "DELETE "
				+ "FROM DocumentEntity AS d WHERE :now > d.expiration") })
public class DocumentEntity implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final String ALL = "dss.doc.all";
	public static final String REMOVE_EXPIRED = "dss.doc.rem.expired";

	private String id;

	private String contentType;

	private byte[] data;

	private Date expiration;

	public DocumentEntity() {
		super();
	}

	public DocumentEntity(String id, String contentType, byte[] data,
			Date expiration) {

		this.id = id;
		this.contentType = contentType;
		this.data = data;
		this.expiration = expiration;
	}

	@Id
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	@Column(length = 100 * 1024, nullable = false)
	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public Date getExpiration() {
		return expiration;
	}

	public void setExpiration(Date expiration) {
		this.expiration = expiration;
	}

	@SuppressWarnings("unchecked")
	public static List<DocumentEntity> getAll(EntityManager entityManager) {
		Query query = entityManager.createNamedQuery(ALL);
		return query.getResultList();
	}

	public static int removeExpired(EntityManager entityManager) {
		Date now = new Date();
		Query query = entityManager.createNamedQuery(REMOVE_EXPIRED)
				.setParameter("now", now);
		return query.executeUpdate();
	}
}
