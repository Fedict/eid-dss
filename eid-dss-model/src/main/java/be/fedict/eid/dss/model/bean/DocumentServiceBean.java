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


import be.fedict.eid.dss.entity.DocumentEntity;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.DocumentService;
import be.fedict.eid.dss.model.exception.DocumentNotFoundException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Stateless
public class DocumentServiceBean implements DocumentService {

    private static final Log LOG = LogFactory.getLog(DocumentServiceBean.class);

    @EJB
    private Configuration configuration;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * {@inheritDoc}
     */
    public DateTime store(String documentId, byte[] data, String contentType) {

        LOG.debug("store document: " + documentId + " data=" + data);

        DateTime expiration = getExpiration();
        DocumentEntity document = new DocumentEntity(documentId, contentType,
                data, expiration.getMillis());
        this.entityManager.persist(document);
        return expiration;
    }

    /**
     * {@inheritDoc}
     */
    public DocumentEntity find(String documentId) {

        LOG.debug("find document: " + documentId);

        DocumentEntity document = this.entityManager.find(DocumentEntity.class,
                documentId);
        if (null == document) {
            return null;
        }

        if (isExpired(document)) {
            LOG.debug("document " + documentId + " is expired, removing...");
            remove(document);
            return null;
        }
        return document;
    }

    /**
     * {@inheritDoc}
     */
    public DocumentEntity retrieve(String documentId) {

        LOG.debug("retrieve document: " + documentId);

        DocumentEntity document = find(documentId);
        if (null == document) {
            return null;
        }

        // remove from storage
        remove(document);
        return document;
    }

    /**
     * {@inheritDoc}
     */
    public DocumentEntity update(String documentId, byte[] data) throws DocumentNotFoundException {

        LOG.debug("update document: " + documentId);

        DocumentEntity document = find(documentId);
        if (null == document) {
            throw new DocumentNotFoundException();
        }
        document.setData(data);
        return document;
    }

    private void remove(DocumentEntity document) {

        LOG.debug("remove document: " + document.getId());
        DocumentEntity attachedDocument =
                this.entityManager.find(DocumentEntity.class, document.getId());
        this.entityManager.remove(attachedDocument);
    }

    private boolean isExpired(DocumentEntity document) {

        return new DateTime(document.getExpiration(),
                ISOChronology.getInstanceUTC()).isBeforeNow();
    }

    private DateTime getExpiration() {

        Integer documentStorageExpiration =
                this.configuration.getValue(
                        ConfigProperty.DOCUMENT_STORAGE_EXPIRATION, Integer.class);

        if (null == documentStorageExpiration || documentStorageExpiration <= 0) {
            throw new RuntimeException("Invalid document storage validity: " +
                    documentStorageExpiration);
        }

        return new DateTime().plus(documentStorageExpiration * 60 * 1000)
                .toDateTime(ISOChronology.getInstanceUTC());

    }
}
