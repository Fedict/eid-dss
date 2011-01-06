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

package be.fedict.eid.dss.model;

import java.io.InputStream;
import java.util.List;

import javax.ejb.Local;

import be.fedict.eid.dss.entity.XmlStyleSheetEntity;
import be.fedict.eid.dss.model.exception.ExistingXmlStyleSheetException;

@Local
public interface XmlStyleSheetManager {

	List<XmlStyleSheetEntity> getXmlStyleSheets();

	void add(String namespace, String revision, InputStream xslInputStream)
			throws ExistingXmlStyleSheetException;

	void delete(String namespace);

	byte[] getXmlStyleSheet(String namespace);

	List<String> getXmlStyleSheetNamespaces();
}
