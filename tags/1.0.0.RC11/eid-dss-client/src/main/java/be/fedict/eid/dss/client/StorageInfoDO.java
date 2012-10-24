/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009-2011 FedICT.
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

package be.fedict.eid.dss.client;

import java.util.Date;

/**
 * Document Storage information container class.
 * 
 * @author Wim Vandenhaute
 */
public class StorageInfoDO {

	private final String artifact;
	private final Date notBefore;
	private final Date notAfter;

	/**
	 * Main constructor.
	 * 
	 * @param artifact the artifact ID
	 * @param notBefore lifetime of artifact in DSS temporary document repository
	 * @param notAfter lifetime of artifact in DSS temporary document repository
	 */
	public StorageInfoDO(String artifact, Date notBefore, Date notAfter) {

		this.artifact = artifact;
		this.notBefore = notBefore;
		this.notAfter = notAfter;
	}

	/**
	 * Gives back the document ID.
	 * <p/>
	 * This document identifier should be used to refer to the document when
	 * creating a signature and when retrieving the signed document.
	 * 
	 * @return the document Id as string.
	 */
	public String getArtifact() {
		return this.artifact;
	}

	/**
	 * Indicates when the eID DSS temporary document repository will have the
	 * document available.
	 * 
	 * @return not before.
	 */
	public Date getNotBefore() {
		return this.notBefore;
	}

	/**
	 * Indicates when the eID DSS will remove the document from its temporary
	 * document repository.
	 * 
	 * @return not after.
	 */
	public Date getNotAfter() {
		return this.notAfter;
	}
}
