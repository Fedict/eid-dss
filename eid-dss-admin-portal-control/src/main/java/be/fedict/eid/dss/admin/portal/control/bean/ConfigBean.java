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

package be.fedict.eid.dss.admin.portal.control.bean;

import be.fedict.eid.dss.admin.portal.control.AdminConstants;
import be.fedict.eid.dss.admin.portal.control.Config;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.TSPDigestAlgo;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;

@Stateful
@Name("dssConfig")
@LocalBinding(jndiBinding = AdminConstants.ADMIN_JNDI_CONTEXT + "ConfigBean")
public class ConfigBean implements Config {

    @Logger
    private Log log;

    @EJB
    private Configuration configuration;

    private String xkmsUrl;

    private String tspUrl;

    private String tspPolicyOid;

    private Boolean httpProxy;

    private String httpProxyHost;

    private Integer httpProxyPort;

    private TSPDigestAlgo tspDigestAlgo;

    private String signTrustDomain;

    private String verifyTrustDomain;

    private String identityTrustDomain;

    private String tsaTrustDomain;

    @Override
    @PostConstruct
    public void postConstruct() {
        this.log.debug("postConstruct");
        this.xkmsUrl = this.configuration.getValue(ConfigProperty.XKMS_URL,
                String.class);

        this.tspUrl = this.configuration.getValue(ConfigProperty.TSP_URL,
                String.class);
        this.tspPolicyOid = this.configuration.getValue(
                ConfigProperty.TSP_POLICY_OID, String.class);
        this.tspDigestAlgo = this.configuration.getValue(
                ConfigProperty.TSP_DIGEST_ALGO, TSPDigestAlgo.class);

        this.httpProxy = this.configuration.getValue(
                ConfigProperty.HTTP_PROXY_ENABLED, Boolean.class);
        this.httpProxyHost = this.configuration.getValue(
                ConfigProperty.HTTP_PROXY_HOST, String.class);
        this.httpProxyPort = this.configuration.getValue(
                ConfigProperty.HTTP_PROXY_PORT, Integer.class);

        this.signTrustDomain = this.configuration.getValue(
                ConfigProperty.SIGN_TRUST_DOMAIN, String.class);
        this.verifyTrustDomain = this.configuration.getValue(
                ConfigProperty.VERIFY_TRUST_DOMAIN, String.class);
        this.identityTrustDomain = this.configuration.getValue(
                ConfigProperty.IDENTITY_TRUST_DOMAIN, String.class);
        this.tsaTrustDomain = this.configuration.getValue(
                ConfigProperty.TSA_TRUST_DOMAIN, String.class);
    }

    @Remove
    @Destroy
    @Override
    public void destroy() {
        this.log.debug("destroy");
    }

    @Override
    public String save() {
        this.log.debug("save");
        this.configuration.setValue(ConfigProperty.XKMS_URL, this.xkmsUrl);

        this.configuration.setValue(ConfigProperty.TSP_URL, this.tspUrl);
        this.configuration.setValue(ConfigProperty.TSP_POLICY_OID,
                this.tspPolicyOid);
        this.configuration.setValue(ConfigProperty.TSP_DIGEST_ALGO,
                this.tspDigestAlgo);

        this.configuration.setValue(ConfigProperty.HTTP_PROXY_ENABLED,
                this.httpProxy);
        this.configuration.setValue(ConfigProperty.HTTP_PROXY_HOST,
                this.httpProxyHost);
        this.configuration.setValue(ConfigProperty.HTTP_PROXY_PORT,
                this.httpProxyPort);

        this.configuration.setValue(ConfigProperty.SIGN_TRUST_DOMAIN,
                this.signTrustDomain);
        this.configuration.setValue(ConfigProperty.VERIFY_TRUST_DOMAIN,
                this.verifyTrustDomain);
        this.configuration.setValue(ConfigProperty.IDENTITY_TRUST_DOMAIN,
                this.identityTrustDomain);
        this.configuration.setValue(ConfigProperty.TSA_TRUST_DOMAIN,
                this.tsaTrustDomain);
        return null;
    }

    @Override
    public String getXkmsUrl() {
        return this.xkmsUrl;
    }

    @Override
    public void setXkmsUrl(String xkmsUrl) {
        this.xkmsUrl = xkmsUrl;
    }

    @Override
    public String getTspUrl() {
        return this.tspUrl;
    }

    @Override
    public void setTspUrl(String tspUrl) {
        this.tspUrl = tspUrl;
    }

    @Override
    public String getTspPolicyOid() {
        return this.tspPolicyOid;
    }

    @Override
    public void setTspPolicyOid(String tspPolicyOid) {
        this.tspPolicyOid = tspPolicyOid;
    }

    @Override
    public Boolean getHttpProxy() {
        return this.httpProxy;
    }

    @Override
    public void setHttpProxy(Boolean httpProxy) {
        this.httpProxy = httpProxy;
    }

    @Override
    public String getHttpProxyHost() {
        return this.httpProxyHost;
    }

    @Override
    public void setHttpProxyHost(String httpProxyHost) {
        this.httpProxyHost = httpProxyHost;
    }

    @Override
    public Integer getHttpProxyPort() {
        return this.httpProxyPort;
    }

    @Override
    public void setHttpProxyPort(Integer httpProxyPort) {
        this.httpProxyPort = httpProxyPort;
    }

    @Override
    public TSPDigestAlgo[] getTspDigestAlgoArray() {
        return TSPDigestAlgo.values();
    }

    @Override
    public TSPDigestAlgo getTspDigestAlgo() {
        return this.tspDigestAlgo;
    }

    @Override
    public void setTspDigestAlgo(TSPDigestAlgo tspDigestAlgo) {
        this.tspDigestAlgo = tspDigestAlgo;
    }

    @Override
    public String getSignTrustDomain() {
        return this.signTrustDomain;
    }

    @Override
    public void setSignTrustDomain(String signTrustDomain) {
        this.signTrustDomain = signTrustDomain;
    }

    @Override
    public String getVerifyTrustDomain() {
        return this.verifyTrustDomain;
    }

    @Override
    public void setVerifyTrustDomain(String verifyTrustDomain) {
        this.verifyTrustDomain = verifyTrustDomain;
    }

    @Override
    public String getIdentityTrustDomain() {
        return this.identityTrustDomain;
    }

    @Override
    public void setIdentityTrustDomain(String identityTrustDomain) {
        this.identityTrustDomain = identityTrustDomain;
    }

    @Override
    public String getTsaTrustDomain() {
        return this.tsaTrustDomain;
    }

    @Override
    public void setTsaTrustDomain(String tsaTrustDomain) {
        this.tsaTrustDomain = tsaTrustDomain;
    }
}
