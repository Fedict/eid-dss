/*
 * eID Identity Provider Project.
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

package be.fedict.eid.dss.model.bean;

import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import be.fedict.eid.dss.entity.RPEntity;
import be.fedict.eid.dss.model.RPService;

@Stateless
public class RPServiceBean implements RPService {

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@inheritDoc}
	 */
	public List<RPEntity> listRPs() {

		return RPEntity.listRPs(this.entityManager);
	}

	/**
	 * {@inheritDoc}
	 */
	public void remove(RPEntity rp) {

		RPEntity attachedRp = this.entityManager.find(RPEntity.class,
				rp.getId());
		this.entityManager.remove(attachedRp);
	}

	/**
	 * {@inheritDoc}
	 */
	public RPEntity save(RPEntity rp) {

		RPEntity attachedRp = null;
		if (null != rp.getId()) {
			attachedRp = this.entityManager.find(RPEntity.class, rp.getId());
		}
		if (null != attachedRp) {
			// save

			// configuration
			attachedRp.setName(rp.getName());
			attachedRp.setRequestSigningRequired(rp.isRequestSigningRequired());
			if (null != rp.getDomain() && rp.getDomain().trim().isEmpty()) {
				attachedRp.setDomain(null);
			} else {
				attachedRp.setDomain(rp.getDomain().trim());
			}

			// logo
			if (null != rp.getLogo()) {
				attachedRp.setLogo(rp.getLogo());
			}

			// signing
			attachedRp.setEncodedCertificate(rp.getEncodedCertificate());

			return attachedRp;
		} else {
			// add
			if (null != rp.getDomain() && rp.getDomain().trim().isEmpty()) {
				rp.setDomain(null);
			}
			this.entityManager.persist(rp);
			return rp;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public RPEntity find(String domain) {

		if (null != domain) {
			return RPEntity.findRP(this.entityManager, domain);
		}

		return null;
	}
}
