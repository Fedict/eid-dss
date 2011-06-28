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

package be.fedict.eid.dss.model.bean;

import be.fedict.eid.dss.model.TrustValidationService;
import be.fedict.eid.dss.model.XmlSchemaManager;
import be.fedict.eid.dss.model.XmlStyleSheetManager;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.tsp.TimeStampToken;

import java.security.cert.X509CRL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;

/**
 * Implementation of DSS document context.
 *
 * @author Frank Cornelis
 */
public class ModelDSSDocumentContext implements DSSDocumentContext {

    private static final long serialVersionUID = 1L;

    private final XmlSchemaManager xmlSchemaManager;

    private final XmlStyleSheetManager xmlStyleSheetManager;

    private final TrustValidationService trustValidationService;

    /**
     * {@inheritDoc}
     */
    public ModelDSSDocumentContext(XmlSchemaManager xmlSchemaManager,
                                   XmlStyleSheetManager xmlStyleSheetManager,
                                   TrustValidationService trustValidationService) {

        this.xmlSchemaManager = xmlSchemaManager;
        this.xmlStyleSheetManager = xmlStyleSheetManager;
        this.trustValidationService = trustValidationService;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getXmlSchema(String namespace) {

        return this.xmlSchemaManager.getXmlSchema(namespace);
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getXmlStyleSheet(String namespace) {

        return this.xmlStyleSheetManager.getXmlStyleSheet(namespace);
    }

    /**
     * {@inheritDoc}
     */
    public void validate(List<X509Certificate> certificateChain,
                         Date validationDate, List<OCSPResp> ocspResponses,
                         List<X509CRL> crls) throws Exception {

        this.trustValidationService.validate(certificateChain, validationDate,
                ocspResponses, crls);
    }

    /**
     * {@inheritDoc}
     */
    public void validate(TimeStampToken timeStampToken) throws Exception {

        this.trustValidationService.validate(timeStampToken);
    }
}
