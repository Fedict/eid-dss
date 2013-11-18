/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2011 FedICT.
 * Copyright (C) 2011 Frank Cornelis.
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

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;

import org.jboss.ejb3.annotation.Depends;

import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.model.MailManager;

@Stateless
@Depends("org.hornetq:module=JMS,name=\"DSSTaskQueue\",type=Queue")
public class MailManagerBean implements MailManager {

	@EJB
	private Configuration configuration;

	@Resource(mappedName = "java:JmsXA")
	private QueueConnectionFactory queueConnectionFactory;

	@Resource(mappedName = TaskMDB.QUEUE_NAME)
	private Queue queue;

	public static final String TASK_NAME_PROPERTY = "TaskName";

	public boolean sendSignedDocumentEnabled() {
		Boolean mailSignedDocument = this.configuration.getValue(
				ConfigProperty.MAIL_SIGNED_DOCUMENT, Boolean.class);
		if (null == mailSignedDocument) {
			return false;
		}
		return mailSignedDocument;
	}

	public void sendSignedDocument(String email, String language,
			String mimetype, byte[] document) {
		if (false == sendSignedDocumentEnabled()) {
			return;
		}
		if (null == email) {
			return;
		}
		if (email.trim().isEmpty()) {
			return;
		}
		if (null == document) {
			return;
		}
		MailSignedDocumentTaskMessage taskMessage = new MailSignedDocumentTaskMessage(
				email, language, mimetype, document);
		sendMessage(taskMessage);
	}

	private void sendMessage(TaskMessage taskMessage) {
		Task taskAnnotation = taskMessage.getClass().getAnnotation(Task.class);
		if (null == taskAnnotation) {
			throw new IllegalArgumentException(
					"task message should be @Task annotated: "
							+ taskMessage.getClass().getName());
		}
		String taskName = taskAnnotation.value();
		try {
			QueueConnection queueConnection = this.queueConnectionFactory
					.createQueueConnection();
			try {
				QueueSession queueSession = queueConnection.createQueueSession(
						true, Session.AUTO_ACKNOWLEDGE);
				try {
					Message message = taskMessage.getMessage(queueSession);
					if (null != message.getStringProperty(TASK_NAME_PROPERTY)) {
						throw new RuntimeException(TASK_NAME_PROPERTY
								+ " property already defined on JMS Message: "
								+ taskMessage.getClass().getName());
					}
					message.setStringProperty(TASK_NAME_PROPERTY, taskName);
					QueueSender queueSender = queueSession
							.createSender(this.queue);
					try {
						queueSender.send(message);
					} finally {
						queueSender.close();
					}
				} finally {
					queueSession.close();
				}
			} finally {
				queueConnection.close();
			}
		} catch (JMSException e) {
			throw new RuntimeException("could not send JMS message "
					+ taskMessage.getClass().getSimpleName() + ": "
					+ e.getMessage(), e);
		}
	}
}
