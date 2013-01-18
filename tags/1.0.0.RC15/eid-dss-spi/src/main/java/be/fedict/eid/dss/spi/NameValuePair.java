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

/**
 * A name value pair class.
 * 
 * @author Frank Cornelis
 */
package be.fedict.eid.dss.spi;

import java.io.Serializable;

/**
 * Generic name-value pair class.
 * 
 * @author Frank Cornelis
 * 
 */
public class NameValuePair implements Serializable {
	private static final long serialVersionUID = 1L;

	private final String name;
	private final String value;

	/**
	 * Main constructor.
	 * 
	 * @param name
	 * @param value
	 */
	public NameValuePair(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Gives back the name of this name-value pair.
	 * 
	 * @return
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Gives back the value of this name-value pair.
	 * 
	 * @return
	 */
	public String getValue() {
		return this.value;
	}
}