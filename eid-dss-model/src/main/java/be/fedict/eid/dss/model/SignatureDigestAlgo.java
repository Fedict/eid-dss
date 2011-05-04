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

package be.fedict.eid.dss.model;

public enum SignatureDigestAlgo {

    SHA1("SHA-1"),
    SHA256("SHA-256"),
    SHA512("SHA-512"),
    SHA384("SHA-384");
//    TODO: no support exists atm in java 6's XMLDSigRI provider nor in apache's xml-security head for RIPEMD160("RIPEMD160");

    private final String algoId;

    /**
     * @param algoId the digest algorithm
     */
    private SignatureDigestAlgo(String algoId) {
        this.algoId = algoId;
    }

    @Override
    public String toString() {
        return this.algoId;
    }

    public String getAlgoId() {
        return algoId;
    }
}
