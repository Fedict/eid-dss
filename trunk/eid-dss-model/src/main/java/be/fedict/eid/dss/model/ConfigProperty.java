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

/**
 * Enumeration of all possible configuration properties. This enumeration also
 * keeps track of the type of each property.
 *
 * @author Frank Cornelis
 */
public enum ConfigProperty {

    TSP_URL("tsp-url", String.class),
    TSP_POLICY_OID("tsp-policy-oid", String.class),
    TSP_DIGEST_ALGO("tsp-digest-algo", TSPDigestAlgo.class),

    HTTP_PROXY_ENABLED("http-proxy", Boolean.class),
    HTTP_PROXY_HOST("http-proxy-host", String.class),
    HTTP_PROXY_PORT("http-proxy-port", Integer.class),

    XKMS_URL("xkms-url", String.class),

    ACTIVE_IDENTITY("active-identity", String.class),
    KEY_STORE_TYPE("key-store-type", KeyStoreType.class),
    KEY_STORE_PATH("key-store-path", String.class),
    KEY_STORE_SECRET("key-store-secret", String.class),
    KEY_ENTRY_SECRET("key-entry-secret", String.class),
    KEY_ENTRY_ALIAS("key-entry-alias", String.class),

    SIGN_TRUST_DOMAIN("sign-trust-domain", String.class),
    VERIFY_TRUST_DOMAIN("verify-trust-domain", String.class),
    IDENTITY_TRUST_DOMAIN("identity-trust-domain", String.class),
    TSA_TRUST_DOMAIN("tsa-trust-domain", String.class);

    private final String name;

    private final Class<?> type;

    private ConfigProperty(String name, Class<?> type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public Class<?> getType() {
        return this.type;
    }
}
