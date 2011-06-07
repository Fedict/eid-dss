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

package be.fedict.eid.dss.model.bean;

import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.TrustValidationService;
import be.fedict.trust.client.XKMS2Client;
import be.fedict.trust.client.exception.RevocationDataNotFoundException;
import be.fedict.trust.client.exception.TrustDomainNotFoundException;
import be.fedict.trust.client.exception.ValidationFailedException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

@Stateless
public class TrustValidationServiceBean implements TrustValidationService {

        private static final Log LOG = LogFactory
                .getLog(TrustValidationServiceBean.class);

        @EJB
        private Configuration configuration;

        /**
         * {@inheritDoc}
         */
        public void validate(List<X509Certificate> certificateChain,
                             Date validationDate, List<OCSPResp> ocspResponses,
                             List<X509CRL> crls) throws CertificateEncodingException,
                TrustDomainNotFoundException, RevocationDataNotFoundException,
                ValidationFailedException {


                String verifyTrustDomain = this.configuration.getValue(
                        ConfigProperty.VERIFY_TRUST_DOMAIN, String.class);

                LOG.debug("validating certificate chain");
                LOG.debug("number of CRLs: " + crls.size());
                LOG.debug("number of OCSPs: " + ocspResponses.size());
                getXkms2Client().validate(verifyTrustDomain, certificateChain,
                        validationDate, ocspResponses, crls);
        }

        /**
         * {@inheritDoc}
         */
        public void validate(TimeStampToken timeStampToken)
                throws CertificateEncodingException, ValidationFailedException,
                TrustDomainNotFoundException, RevocationDataNotFoundException {

                String tsaTrustDomain = this.configuration.getValue(
                        ConfigProperty.TSA_TRUST_DOMAIN, String.class);

                LOG.debug("validating timestamp token");
                getXkms2Client().validate(tsaTrustDomain, timeStampToken);
        }

        /**
         * {@inheritDoc}
         */
        public XKMS2Client getXkms2Client() {

                String xkmsUrl = this.configuration.getValue(ConfigProperty.XKMS_URL,
                        String.class);
                XKMS2Client xkms2Client = new XKMS2Client(xkmsUrl);

                Boolean useHttpProxy = this.configuration.getValue(
                        ConfigProperty.HTTP_PROXY_ENABLED, Boolean.class);
                if (null != useHttpProxy && useHttpProxy) {
                        String httpProxyHost = this.configuration.getValue(
                                ConfigProperty.HTTP_PROXY_HOST, String.class);
                        int httpProxyPort = this.configuration.getValue(
                                ConfigProperty.HTTP_PROXY_PORT, Integer.class);
                        xkms2Client.setProxy(httpProxyHost, httpProxyPort);
                } else {
                        // disable previously set proxy
                        xkms2Client.setProxy(null, 0);
                }

                return xkms2Client;
        }
}
