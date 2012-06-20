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

import java.util.Collection;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.ScheduleExpression;
import javax.ejb.Stateless;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.joda.time.DateTime;
import org.joda.time.chrono.ISOChronology;

import be.fedict.eid.dss.entity.DocumentEntity;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.DocumentService;
import be.fedict.eid.dss.model.exception.DocumentNotFoundException;
import be.fedict.eid.dss.model.exception.InvalidCronExpressionException;

@Stateless
public class DocumentServiceBean implements DocumentService {

	private static final Log LOG = LogFactory.getLog(DocumentServiceBean.class);

	private static final String TIMER_ID = DocumentServiceBean.class.getName()
			+ ".Timer";

	@EJB
	private Configuration configuration;

	@Resource
	private TimerService timerService;

	@PersistenceContext
	private EntityManager entityManager;

	/**
	 * {@inheritDoc}
	 */
	public DateTime store(String documentId, byte[] data, String contentType) {

		LOG.debug("store document: " + documentId);

		DateTime expiration = getExpiration();
		DocumentEntity document = new DocumentEntity(documentId, contentType,
				data, expiration.toDate());
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
	public DocumentEntity update(String documentId, byte[] data)
			throws DocumentNotFoundException {

		LOG.debug("update document: " + documentId);

		DocumentEntity document = find(documentId);
		if (null == document) {
			throw new DocumentNotFoundException();
		}
		document.setData(data);
		return document;
	}

	/**
	 * {@inheritDoc}
	 */
	public void remove(String documentId) {

		DocumentEntity document = find(documentId);
		if (null != document) {
			remove(document);
		}
	}

	private void remove(DocumentEntity document) {

		LOG.debug("remove document: " + document.getId());
		DocumentEntity attachedDocument = this.entityManager.find(
				DocumentEntity.class, document.getId());
		this.entityManager.remove(attachedDocument);
	}

	private boolean isExpired(DocumentEntity document) {

		return new DateTime(document.getExpiration(),
				ISOChronology.getInstanceUTC()).isBeforeNow();
	}

	private DateTime getExpiration() {

		Integer documentStorageExpiration = this.configuration.getValue(
				ConfigProperty.DOCUMENT_STORAGE_EXPIRATION, Integer.class);

		if (null == documentStorageExpiration || documentStorageExpiration <= 0) {
			throw new RuntimeException("Invalid document storage validity: "
					+ documentStorageExpiration);
		}

		return new DateTime().plus(documentStorageExpiration * 60 * 1000)
				.toDateTime(ISOChronology.getInstanceUTC());

	}

	/**
	 * {@inheritDoc}
	 */
	@Timeout
	public void timeOut(Timer timer) {

		String timerInfo = (String) timer.getInfo();
		LOG.debug("timeout: " + timerInfo);
		if (null == timerInfo) {
			LOG.error("no timer info ?? cancel timer");
			timer.cancel();
			return;
		}

		if (timerInfo.equals(TIMER_ID)) {
			cleanup();
			LOG.debug("Next cleanup: " + timer.getNextTimeout());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void startTimer() throws InvalidCronExpressionException {

		String cronSchedule = this.configuration.getValue(
				ConfigProperty.DOCUMENT_CLEANUP_TASK_SCHEDULE, String.class);
		startTimer(cronSchedule);
	}

	/**
	 * {@inheritDoc}
	 */
	public void startTimer(String cronSchedule)
			throws InvalidCronExpressionException {

		LOG.debug("start document service's cleanup task timer");

		if (null == cronSchedule || cronSchedule.isEmpty()) {
			// TODO: error message sufficient? or explode here?...
			LOG.error("No interval set for document service cleanup task!");
			return;
		}

		// remove old timers
		cancelTimers();

		TimerConfig timerConfig = new TimerConfig();
		timerConfig.setInfo(TIMER_ID);
		timerConfig.setPersistent(true);

		ScheduleExpression schedule = getScheduleExpression(cronSchedule);

		Timer timer;
		try {
			timer = this.timerService
					.createCalendarTimer(schedule, timerConfig);
		} catch (Exception e) {
			LOG.error("Exception while creating timer for document service "
					+ "cleanup task: " + e.getMessage(), e);
			throw new InvalidCronExpressionException(e);
		}

		LOG.debug("created timer for document service cleanup task: next="
				+ timer.getNextTimeout().toString());
	}

	public void cancelTimers() {

		Collection<Timer> timers = this.timerService.getTimers();
		for (Timer timer : timers) {
			if (timer.getInfo() != null) {
				if (timer.getInfo().equals(TIMER_ID)) {
					timer.cancel();
					LOG.debug("cancel timer: " + TIMER_ID);
				}
			}
		}
	}

	private ScheduleExpression getScheduleExpression(String cronSchedule) {

		ScheduleExpression schedule = new ScheduleExpression();
		String[] fields = cronSchedule.split(" ");
		if (fields.length > 8) {
			throw new IllegalArgumentException(
					"Too many fields in cronexpression: " + cronSchedule);
		}
		if (fields.length > 1) {
			schedule.second(fields[0]);
		}
		if (fields.length > 2) {
			schedule.minute(fields[1]);
		}
		if (fields.length > 3) {
			schedule.hour(fields[2]);
		}
		if (fields.length > 4) {
			schedule.dayOfMonth(fields[3]);
		}
		if (fields.length > 5) {
			schedule.month(fields[4]);
		}
		if (fields.length > 6) {
			schedule.dayOfWeek(fields[5]);
		}
		if (fields.length > 7) {
			schedule.year(fields[6]);
		}

		return schedule;
	}

	/**
	 * {@inheritDoc}
	 */
	public int cleanup() {

		LOG.debug("document cleanup");
		int removals = DocumentEntity.removeExpired(this.entityManager);
		LOG.debug("# of removals: " + removals);
		return removals;
	}
}
