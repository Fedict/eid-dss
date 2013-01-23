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

import javax.ejb.Local;
import javax.ejb.Timer;

import org.joda.time.DateTime;

import be.fedict.eid.dss.entity.DocumentEntity;
import be.fedict.eid.dss.model.exception.DocumentNotFoundException;
import be.fedict.eid.dss.model.exception.InvalidCronExpressionException;

/**
 * Interface for the document service. The document service maintains the
 * temporaryily stored DSS Documents
 * 
 * @author Wim Vandenhaute
 */
@Local
public interface DocumentService {

	/**
	 * Store specified document.
	 * 
	 * @param documentId
	 *            id of the stored document entry
	 * @param data
	 *            document data
	 * @param contentType
	 *            content type of the document
	 * @return the expiration date of the stored document
	 */
	DateTime store(String documentId, byte[] data, String contentType);

	/**
	 * @param documentId
	 *            id of the document to find.
	 * @return the document with specified document ID. If not existing or
	 *         expired returns <code>null</code>
	 */
	DocumentEntity find(String documentId);

	/**
	 * Finds and removes the document from storae if found.
	 * 
	 * @param documentId
	 *            id of the document to find.
	 * @return the document with specified document ID. If not existing or
	 *         expired returns <code>null</code>
	 */
	DocumentEntity retrieve(String documentId);

	/**
	 * Update document entity with specified ID its data
	 * 
	 * @param documentId
	 *            if of document to update
	 * @param data
	 *            data of document
	 * @return the updated document entity
	 * @throws DocumentNotFoundException
	 *             the document was not found.
	 */
	DocumentEntity update(String documentId, byte[] data)
			throws DocumentNotFoundException;

	/**
	 * Remove specified document if present.
	 * 
	 * @param documentId
	 *            the ID of the document to remove
	 */
	void remove(String documentId);

	/**
	 * Timer has timeout, fire cleanup.
	 * 
	 * @param timer
	 *            the timer that has timed out.
	 */
	void timeOut(Timer timer);

	/**
	 * Start the timer for the cleanup "task" of the document service.
	 * 
	 * @throws InvalidCronExpressionException
	 *             Invalid cron schedule, timer was not started.
	 */
	void startTimer() throws InvalidCronExpressionException;

	/**
	 * Start the timer for the cleanup "task" of the document service.
	 * 
	 * @param cronSchedule
	 *            cron schedule
	 * @throws InvalidCronExpressionException
	 *             Invalid cron schedule, timer was not started.
	 */
	void startTimer(String cronSchedule) throws InvalidCronExpressionException;

	/**
	 * Cleanup all expired temporary documents.
	 * 
	 * @return the # of removals.
	 */
	int cleanup();

}
