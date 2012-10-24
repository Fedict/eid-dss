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

package be.fedict.eid.dss.model.exception;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Wrapper around exceptions received from Trust Service so we have logging.
 * 
 * @author Frank Cornelis
 * 
 */
public class TrustServiceClientException extends Exception {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(TrustServiceClientException.class);

	public TrustServiceClientException(String message, Throwable cause) {
		super(message, cause);
		LOG.error(
				"eID Trust Service client error: " + message + ": "
						+ cause.getMessage(), cause);
	}
}
