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

package test.unit.be.fedict.eid.dss.document.ooxml;

import be.fedict.eid.applet.service.signer.ooxml.OOXMLProvider;
import be.fedict.eid.dss.document.ooxml.OOXMLDSSDocumentService;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.SignatureInfo;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.BeforeClass;
import org.junit.Test;

import java.security.Security;
import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class OOXMLDSSDocumentServiceTest {

        private static final Log LOG = LogFactory
                .getLog(OOXMLDSSDocumentServiceTest.class);

        @BeforeClass
        public static void setUp() {
                if (null == Security.getProvider(BouncyCastleProvider.PROVIDER_NAME)) {
                        Security.addProvider(new BouncyCastleProvider());
                }
                OOXMLProvider.install();
        }

        @Test
        public void testVerifySignatures() throws Exception {
                // setup
                OOXMLDSSDocumentService testedInstance = new OOXMLDSSDocumentService();
                byte[] document = IOUtils.toByteArray(OOXMLDSSDocumentServiceTest.class
                        .getResourceAsStream("/hello-world-signed.docx"));

                DSSDocumentContext mockContext = EasyMock
                        .createMock(DSSDocumentContext.class);
                Capture<List<X509Certificate>> certificateChainCapture = new Capture<List<X509Certificate>>();
                Capture<Date> validationDateCapture = new Capture<Date>();
                Capture<List<OCSPResp>> ocspResponsesCapture = new Capture<List<OCSPResp>>();
                Capture<List<X509CRL>> crlsCapture = new Capture<List<X509CRL>>();
                Capture<TimeStampToken> timeStampTokenCapture = new Capture<TimeStampToken>();
                mockContext.validate(EasyMock.capture(certificateChainCapture),
                        EasyMock.capture(validationDateCapture),
                        EasyMock.capture(ocspResponsesCapture),
                        EasyMock.capture(crlsCapture));
                mockContext.validate(EasyMock.capture(timeStampTokenCapture));
                mockContext.validate(EasyMock.capture(timeStampTokenCapture));
                expect(mockContext.getTimestampMaxOffset()).andReturn(1000L);

                // prepare
                EasyMock.replay(mockContext);

                // operate
                testedInstance.init(mockContext, "mime-type");
                List<SignatureInfo> signatureInfos = testedInstance
                        .verifySignatures(document);

                // verify
                EasyMock.verify(mockContext);
                assertNotNull(signatureInfos);
                assertEquals(1, signatureInfos.size());
                SignatureInfo signatureInfo = signatureInfos.get(0);
                assertNotNull(signatureInfo.getSigner());
                assertNotNull(signatureInfo.getSigningTime());
                LOG.debug("signing time: " + signatureInfo.getSigningTime());
                assertEquals(signatureInfo.getSigningTime(),
                        validationDateCapture.getValue());
                assertEquals(signatureInfo.getSigner(), certificateChainCapture
                        .getValue().get(0));
                assertEquals(1, ocspResponsesCapture.getValue().size());
                assertEquals(1, crlsCapture.getValue().size());
        }
}
