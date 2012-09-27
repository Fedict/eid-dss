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

import be.fedict.eid.applet.service.signer.DigestAlgo;

/**
 * Enumeration of all possible configuration properties. This enumeration also
 * keeps track of the type of each property.
 * 
 * @author Frank Cornelis
 */
public enum ConfigProperty {

	TSP_URL("tsp-url", String.class, "http://tsa.belgium.be/connect"), TSP_POLICY_OID(
			"tsp-policy-oid", String.class), TSP_DIGEST_ALGO("tsp-digest-algo",
			TSPDigestAlgo.class),

	HTTP_PROXY_ENABLED("http-proxy", Boolean.class), HTTP_PROXY_HOST(
			"http-proxy-host", String.class), HTTP_PROXY_PORT(
			"http-proxy-port", Integer.class),

	XKMS_URL("xkms-url", String.class,
			"https://www.e-contract.be/eid-trust-service-ws/xkms2"),

	ACTIVE_IDENTITY("active-identity", String.class), KEY_STORE_TYPE(
			"key-store-type", KeyStoreType.class), KEY_STORE_PATH(
			"key-store-path", String.class), KEY_STORE_SECRET(
			"key-store-secret", String.class), KEY_ENTRY_SECRET(
			"key-entry-secret", String.class), KEY_ENTRY_ALIAS(
			"key-entry-alias", String.class),

	SIGN_TRUST_DOMAIN("sign-trust-domain", String.class, "BE"), VERIFY_TRUST_DOMAIN(
			"verify-trust-domain", String.class, "BE"), IDENTITY_TRUST_DOMAIN(
			"identity-trust-domain", String.class, "BE-NAT-REG"), TSA_TRUST_DOMAIN(
			"tsa-trust-domain", String.class, "BE-TSA"),

	SIGNATURE_DIGEST_ALGO("signature-digest-algo", DigestAlgo.class,
			DigestAlgo.SHA512),

	DOCUMENT_STORAGE_EXPIRATION("document-storage-expiration", Integer.class, 5), DOCUMENT_CLEANUP_TASK_SCHEDULE(
			"document-cleanup-task-schedule", String.class, "0 0/15 * * * *"),

	/**
	 * We take a default value of 5 minutes. This required because of delay
	 * caused by eID PIN validation, SignatureTimeStamp, eID certificate chain
	 * PKI validation, TSA PKI validation, SigAndRefsTimeStamp.
	 */
	TIMESTAMP_MAX_OFFSET("timestamp-max-offset", Long.class, 5 * 60 * 1000L),

	MAX_GRACE_PERIOD("max-grace-period", Long.class, 24 * 7L),

	SMTP_SERVER("smtp-server", String.class), MAIL_FROM("mail-from",
			String.class), MAIL_PREFIX("mail-prefix", String.class), MAIL_SIGNED_DOCUMENT(
			"mail-signed-document", Boolean.class),

	SECURITY_REMOVE_CARD("security-remove-card", Boolean.class), SECURITY_HSTS(
			"security-hsts", Boolean.class),

	DSS_WS_URL("dss-ws-url", String.class,
			"http://localhost:8080/eid-dss-ws/dss");

	private final String name;

	private final Class<?> type;

	private final Object defaultValue;

	private ConfigProperty(String name, Class<?> type) {
		this.name = name;
		this.type = type;
		this.defaultValue = null;
	}

	private ConfigProperty(String name, Class<?> type, Object defaultValue) {
		this.name = name;
		this.type = type;
		this.defaultValue = defaultValue;
	}

	public String getName() {
		return this.name;
	}

	public Class<?> getType() {
		return this.type;
	}

	public Object getDefaultValue() {
		return this.defaultValue;
	}
}
