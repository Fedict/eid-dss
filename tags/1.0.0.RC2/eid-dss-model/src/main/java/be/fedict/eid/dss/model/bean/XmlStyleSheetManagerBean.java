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

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.io.IOUtils;

import be.fedict.eid.dss.entity.XmlStyleSheetEntity;
import be.fedict.eid.dss.model.XmlStyleSheetManager;
import be.fedict.eid.dss.model.exception.ExistingXmlStyleSheetException;

@Stateless
public class XmlStyleSheetManagerBean implements XmlStyleSheetManager {

	@PersistenceContext
	private EntityManager entityManager;

	public List<XmlStyleSheetEntity> getXmlStyleSheets() {
		return XmlStyleSheetEntity.getAll(this.entityManager);
	}

	public void add(String namespace, String revision,
			InputStream xslInputStream) throws ExistingXmlStyleSheetException {
		XmlStyleSheetEntity existingXmlStyleSheetEntity = this.entityManager
				.find(XmlStyleSheetEntity.class, namespace);
		if (null != existingXmlStyleSheetEntity) {
			throw new ExistingXmlStyleSheetException();
		}
		byte[] xsl;
		try {
			xsl = IOUtils.toByteArray(xslInputStream);
		} catch (IOException e) {
			throw new RuntimeException("IO error: " + e.getMessage(), e);
		}
		XmlStyleSheetEntity xmlStyleSheetEntity = new XmlStyleSheetEntity(
				namespace, revision, xsl);
		this.entityManager.persist(xmlStyleSheetEntity);
	}

	public void delete(String namespace) {
		XmlStyleSheetEntity xmlStyleSheetEntity = this.entityManager.find(
				XmlStyleSheetEntity.class, namespace);
		this.entityManager.remove(xmlStyleSheetEntity);
	}

	public byte[] getXmlStyleSheet(String namespace) {
		XmlStyleSheetEntity xmlStyleSheetEntity = this.entityManager.find(
				XmlStyleSheetEntity.class, namespace);
		if (null == xmlStyleSheetEntity) {
			return null;
		}
		return xmlStyleSheetEntity.getXsl();
	}

	public List<String> getXmlStyleSheetNamespaces() {
		List<XmlStyleSheetEntity> xmlStyleSheets = getXmlStyleSheets();
		List<String> xmlStyleSheetNamespaces = new LinkedList<String>();
		for (XmlStyleSheetEntity xmlStyleSheet : xmlStyleSheets) {
			xmlStyleSheetNamespaces.add(xmlStyleSheet.getNamespace());
		}
		return xmlStyleSheetNamespaces;
	}

}
