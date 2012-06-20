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

package be.fedict.eid.dss.spi;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 * The Browser POST response. This is used to construct a Browser POST to get
 * back to the Service Provider.
 * 
 * @author Frank Cornelis
 */
public class BrowserPOSTResponse implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String actionUrl;
	private final List<NameValuePair> attributes;

	/**
	 * Main constructor.
	 * 
	 * @param actionUrl
	 */
	public BrowserPOSTResponse(String actionUrl) {
		this.actionUrl = actionUrl;
		this.attributes = new LinkedList<NameValuePair>();
	}

	public void addAttribute(String name, String value) {
		NameValuePair attribute = new NameValuePair(name, value);
		this.attributes.add(attribute);
	}

	public String getActionUrl() {
		return this.actionUrl;
	}

	public List<NameValuePair> getAttributes() {
		return this.attributes;
	}
}