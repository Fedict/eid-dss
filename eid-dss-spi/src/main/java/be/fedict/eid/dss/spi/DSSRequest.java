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

package be.fedict.eid.dss.spi;

import java.io.Serializable;

/**
 * Represents a DSS request after unmarshalling by the DSSProtocolService.
 *
 * @author Frank Cornelis
 */
public class DSSRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private final byte[] documentData;
    private final String contentType;

    private final String documentId;

    private final String language;

    /**
     * Main constructor.
     *
     * @param documentData document data, if <code>null</code>
     *                     documentId needs to be specified
     * @param contentType  document content type, if <code>null</code>
     *                     documentId needs to be specified
     * @param documentId   document's ID, if <code>null</code>
     *                     documentData needs to be specified
     * @param language     optional language
     */
    public DSSRequest(byte[] documentData, String contentType, String documentId,
                      String language) {

        this.documentData = documentData;
        this.contentType = contentType;
        this.documentId = documentId;
        this.language = language;
    }

    /**
     * @return the data bytes of the document to be signed.
     */
    public byte[] getDocumentData() {
        return this.documentData;
    }

    /**
     * @return the content type of the document to be signed.
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * @return the language to be used on the GUI for signing the document.
     */
    public String getLanguage() {
        return this.language;
    }

    /**
     * @return the document's ID.
     */
    public String getDocumentId() {
        return documentId;
    }
}
