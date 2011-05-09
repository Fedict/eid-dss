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

package be.fedict.eid.dss.admin.portal.control;

import be.fedict.eid.dss.model.SignatureDigestAlgo;
import be.fedict.eid.dss.model.TSPDigestAlgo;

import javax.ejb.Local;

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

    SignatureDigestAlgo[] getSignatureDigestAlgoArray();

    SignatureDigestAlgo getSignatureDigestAlgo();

    void setSignatureDigestAlgo(SignatureDigestAlgo signatureDigestAlgo);

    Integer getDocumentStorageExpiration();

    void setDocumentStorageExpiration(Integer documentStorageExpiration);

    String getDocumentCleanupTaskCronSchedule();

    void setDocumentCleanupTaskCronSchedule(String documentCleanupTaskCronSchedule);

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
