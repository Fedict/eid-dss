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

    public StorageInfoDO(String artifact, Date notBefore, Date notAfter) {

        this.artifact = artifact;
        this.notBefore = notBefore;
        this.notAfter = notAfter;
    }

    public String getArtifact() {
        return this.artifact;
    }

    public Date getNotBefore() {
        return this.notBefore;
    }

    public Date getNotAfter() {
        return this.notAfter;
    }
}
