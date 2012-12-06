/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010-2012 FedICT.
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

package be.fedict.eid.dss.admin.portal.control;

import javax.ejb.Local;

import be.fedict.eid.applet.service.signer.DigestAlgo;
import be.fedict.eid.dss.model.TSPDigestAlgo;

@Local
public interface Config {

	/*
	 * Accessors.
	 */
	String getXkmsUrl();

	void setXkmsUrl(String xkmsUrl);

	String getTspUrl();

	void setTspUrl(String tspUrl);

	String getTspPolicyOid();

	void setTspPolicyOid(String tspPolicyOid);

	Boolean getHttpProxy();

	void setHttpProxy(Boolean httpProxy);

	String getHttpProxyHost();

	void setHttpProxyHost(String httpProxyHost);

	Integer getHttpProxyPort();

	void setHttpProxyPort(Integer httpProxyPort);

	TSPDigestAlgo[] getTspDigestAlgoArray();

	TSPDigestAlgo getTspDigestAlgo();

	void setTspDigestAlgo(TSPDigestAlgo tspDigestAlgo);

	String getSignTrustDomain();

	void setSignTrustDomain(String signTrustDomain);

	String getVerifyTrustDomain();

	void setVerifyTrustDomain(String verifyTrustDomain);

	String getIdentityTrustDomain();

	void setIdentityTrustDomain(String identityTrustDomain);

	String getTsaTrustDomain();

	void setTsaTrustDomain(String tsaTrustDomain);

	DigestAlgo[] getSignatureDigestAlgoArray();

	DigestAlgo getSignatureDigestAlgo();

	void setSignatureDigestAlgo(DigestAlgo signatureDigestAlgo);

	Integer getDocumentStorageExpiration();

	void setDocumentStorageExpiration(Integer documentStorageExpiration);

	String getDocumentCleanupTaskCronSchedule();

	void setDocumentCleanupTaskCronSchedule(
			String documentCleanupTaskCronSchedule);

	Long getTimestampMaxOffset();

	void setTimestampMaxOffset(Long timestampMaxOffset);

	Long getMaxGracePeriod();

	void setMaxGracePeriod(Long maxGracePeriod);

	Boolean getMailSignedDocument();

	void setMailSignedDocument(Boolean mailSignedDocument);

	String getSmtpServer();

	void setSmtpServer(String smtpServer);

	String getMailFrom();

	void setMailFrom(String mailFrom);

	String getMailPrefix();

	void setMailPrefix(String mailPrefix);

	Boolean getRemoveCard();

	void setRemoveCard(Boolean removeCard);

	Boolean getHsts();

	void setHsts(Boolean hsts);

	String getDssWSUrl();

	void setDssWSUrl(String dssWSUrl);

	/*
	 * Actions.
	 */
	String save();

	/*
	 * Lifecycle.
	 */
	void destroy();

	void postConstruct();
}
