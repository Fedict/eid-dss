/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009 FedICT.
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

import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.time.TSPTimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.applet.service.spi.*;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.DocumentRepository;
import be.fedict.eid.dss.model.ServicesManager;
import be.fedict.eid.dss.spi.DSSDocumentService;
import org.jboss.ejb3.annotation.LocalBinding;

import javax.ejb.EJB;
import javax.ejb.Local;
import javax.ejb.Stateless;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * XML Signature Service bean. Acts as a proxy towards the actual
 * SignatureService implementation provided by some document service.
 * <p/>
 *
 * @author Frank Cornelis
 */
@Stateless
@Local(SignatureServiceEx.class)
@LocalBinding(jndiBinding = "fedict/eid/dss/SignatureServiceBean")
public class SignatureServiceBean implements SignatureServiceEx {

    @EJB
    private Configuration configuration;

    @EJB
    private ServicesManager servicesManager;

    public String getFilesDigestAlgorithm() {
        return null;
    }

    public DigestInfo preSign(List<DigestInfo> digestInfos,
                              List<X509Certificate> signingCertificateChain)
            throws NoSuchAlgorithmException {
        throw new UnsupportedOperationException();
    }

    public void postSign(byte[] signatureValue,
                         List<X509Certificate> signingCertificateChain)
            throws SecurityException {

        SignatureService signatureService = getSignatureService(null, null);
        signatureService.postSign(signatureValue, signingCertificateChain);
    }

    private SignatureServiceEx getSignatureService(IdentityDTO identity,
                                                   byte[] photo) {

        String tspUrl = this.configuration.getValue(ConfigProperty.TSP_URL,
                String.class);
        String tspPolicyOid = this.configuration.getValue(
                ConfigProperty.TSP_POLICY_OID, String.class);

        Boolean useHttpProxy = this.configuration.getValue(
                ConfigProperty.HTTP_PROXY_ENABLED, Boolean.class);
        String httpProxyHost;
        int httpProxyPort;
        if (null != useHttpProxy && useHttpProxy) {
            httpProxyHost = this.configuration.getValue(
                    ConfigProperty.HTTP_PROXY_HOST, String.class);
            httpProxyPort = this.configuration.getValue(
                    ConfigProperty.HTTP_PROXY_PORT, Integer.class);
        } else {
            httpProxyHost = null;
            httpProxyPort = 0;
        }

        String xkmsUrl = this.configuration.getValue(ConfigProperty.XKMS_URL,
                String.class);
        String signTrustDomain = this.configuration.getValue(
                ConfigProperty.SIGN_TRUST_DOMAIN, String.class);
        String tsaTrustDomain = this.configuration.getValue(
                ConfigProperty.TSA_TRUST_DOMAIN, String.class);

        RevocationDataService revocationDataService = new TrustServiceRevocationDataService(
                xkmsUrl, httpProxyHost, httpProxyPort, signTrustDomain);
        SignatureFacet signatureFacet = new SignerCertificateSignatureFacet();
        TimeStampServiceValidator timeStampServiceValidator = new TrustServiceTimeStampServiceValidator(
                xkmsUrl, httpProxyHost, httpProxyPort, tsaTrustDomain);
        TSPTimeStampService timeStampService = new TSPTimeStampService(tspUrl,
                timeStampServiceValidator);
        if (null != httpProxyHost) {
            timeStampService.setProxy(httpProxyHost, httpProxyPort);
        }
        if (null != tspPolicyOid && !tspPolicyOid.isEmpty()) {
            timeStampService.setRequestPolicy(tspPolicyOid);
        }

        HttpSession httpSession = HttpSessionTemporaryDataStorage
                .getHttpSession();
        DocumentRepository documentRepository = new DocumentRepository(
                httpSession);
        byte[] document = documentRepository.getDocument();
        ByteArrayInputStream documentInputStream = new ByteArrayInputStream(
                document);
        String role = documentRepository.getRole();

        OutputStream documentOutputStream = new DocumentRepositoryOutputStream();

        DSSDocumentService documentService = this.servicesManager
                .getDocumentService();
        SignatureServiceEx signatureService;
        try {
            signatureService = documentService
                    .getSignatureService(documentInputStream, timeStampService,
                            timeStampServiceValidator, revocationDataService,
                            signatureFacet, documentOutputStream, role,
                            identity, photo);
        } catch (Exception e) {
            throw new RuntimeException("error retrieving signature service: "
                    + e.getMessage(), e);
        }
        return signatureService;
    }

    public DigestInfo preSign(List<DigestInfo> digestInfos,
                              List<X509Certificate> signingCertificateChain,
                              IdentityDTO identity, AddressDTO address, byte[] photo)
            throws NoSuchAlgorithmException {

        SignatureServiceEx signatureService = getSignatureService(identity,
                photo);
        return signatureService.preSign(digestInfos, signingCertificateChain,
                identity, address, photo);
    }
}
