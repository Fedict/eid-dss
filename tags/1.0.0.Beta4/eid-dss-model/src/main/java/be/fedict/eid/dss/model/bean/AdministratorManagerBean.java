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

import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.codec.digest.DigestUtils;

import be.fedict.eid.dss.entity.AdministratorEntity;
import be.fedict.eid.dss.model.AdministratorManager;

@Stateless
public class AdministratorManagerBean implements AdministratorManager {

	@PersistenceContext
	private EntityManager entityManager;

	public boolean hasAdminRights(X509Certificate certificate) {
		String id = getId(certificate);
		AdministratorEntity adminEntity = this.entityManager.find(
				AdministratorEntity.class, id);
		if (null != adminEntity) {
			if (false == adminEntity.isPending()) {
				return true;
			} else {
				/*
				 * This admin is awaiting approval.
				 */
				return false;
			}
		}
		String name = certificate.getSubjectX500Principal().toString();
		if (AdministratorEntity.hasAdmins(this.entityManager)) {
			/*
			 * So we register this admin as pending.
			 */
			adminEntity = new AdministratorEntity(id, name, true);
			this.entityManager.persist(adminEntity);
			return false;
		}
		/*
		 * Else we bootstrap the admin.
		 */
		adminEntity = new AdministratorEntity(id, name, false);
		this.entityManager.persist(adminEntity);
		return true;
	}

	private String getId(X509Certificate certificate) {
		PublicKey publicKey = certificate.getPublicKey();
		String id = DigestUtils.shaHex(publicKey.getEncoded());
		return id;
	}

	public List<AdministratorEntity> listAdmins() {
		return AdministratorEntity.getAdmins(this.entityManager);
	}

	public void removeAdmin(String adminId) {
		AdministratorEntity adminEntity = this.entityManager.find(
				AdministratorEntity.class, adminId);
		this.entityManager.remove(adminEntity);
	}

	public void approveAdmin(String adminId) {
		AdministratorEntity adminEntity = this.entityManager.find(
				AdministratorEntity.class, adminId);
		adminEntity.setPending(false);
	}
}
