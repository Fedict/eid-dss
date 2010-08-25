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

package be.fedict.eid.dss.model;

import java.util.Map;

import javax.ejb.Local;

/**
 * Interface for component that manages the registered different services.
 * 
 * @author Frank Cornelis
 * 
 */
@Local
public interface ServicesManager {

	/**
	 * Gives back a map with (context path, protocol service class name) tuples.
	 * 
	 * @return
	 */
	Map<String, String> getProtocolServiceClassNames();

	/**
	 * Gives back a map with (content type, document service class name) tuples.
	 * 
	 * @return
	 */
	Map<String, String> getDocumentServiceClassNames();

}
