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

import static be.fedict.eid.dss.model.bean.TaskMDB.QUEUE_NAME;

import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;

@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),
		@ActivationConfigProperty(propertyName = "destination", propertyValue = QUEUE_NAME) })
public class TaskMDB implements MessageListener {

	private static final Log LOG = LogFactory.getLog(TaskMDB.class);

	public static final String QUEUE_NAME = "queue/dss/task-queue";

	@EJB
	private Configuration configuration;

	public void onMessage(Message message) {
		LOG.debug("onMessage");
		String taskName;
		try {
			taskName = message
					.getStringProperty(MailManagerBean.TASK_NAME_PROPERTY);
		} catch (JMSException e) {
			throw new RuntimeException("JMS error: " + e.getMessage(), e);
		}

		if (MailSignedDocumentTaskMessage.TASK_NAME.equals(taskName)) {
			MailSignedDocumentTaskMessage mailSignedDocumentTaskMessage = new MailSignedDocumentTaskMessage(
					message);
			processMailSignedDocumentTask(mailSignedDocumentTaskMessage);
		} else {
			LOG.error("unknown task: " + taskName);
		}
	}

	private void processMailSignedDocumentTask(
			MailSignedDocumentTaskMessage mailSignedDocumentTaskMessage) {
		String mailTo = mailSignedDocumentTaskMessage.getEmail();
		String mimetype = mailSignedDocumentTaskMessage.getMimetype();
		byte[] attachment = mailSignedDocumentTaskMessage.getDocument();
		sendMail(mailTo, "Signed Document from eID DSS",
				"The signed document can be found within the attachment.",
				mimetype, attachment);
	}

	private void sendMail(String mailTo, String subject, String messageBody,
			String attachmentMimetype, byte[] attachment) {
		LOG.debug("sending email to " + mailTo + " with subject \"" + subject
				+ "\"");
		String smtpServer = this.configuration.getValue(
				ConfigProperty.SMTP_SERVER, String.class);
		if (null == smtpServer || smtpServer.trim().isEmpty()) {
			LOG.warn("no SMTP server configured");
			return;
		}
		String mailFrom = this.configuration.getValue(ConfigProperty.MAIL_FROM,
				String.class);
		if (null == mailFrom || mailFrom.trim().isEmpty()) {
			LOG.warn("no mail from address configured");
			return;
		}
		LOG.debug("mail from: " + mailFrom);

		Properties props = new Properties();
		props.put("mail.smtp.host", smtpServer);
		props.put("mail.from", mailFrom);

		String mailPrefix = this.configuration.getValue(
				ConfigProperty.MAIL_PREFIX, String.class);
		if (null != mailPrefix && false == mailPrefix.trim().isEmpty()) {
			subject = "[" + mailPrefix.trim() + "] " + subject;
		}

		Session session = Session.getInstance(props, null);
		try {
			MimeMessage mimeMessage = new MimeMessage(session);
			mimeMessage.setFrom();
			mimeMessage.setRecipients(RecipientType.TO, mailTo);
			mimeMessage.setSubject(subject);
			mimeMessage.setSentDate(new Date());

			MimeBodyPart mimeBodyPart = new MimeBodyPart();
			mimeBodyPart.setText(messageBody);

			Multipart multipart = new MimeMultipart();
			// first part is body
			multipart.addBodyPart(mimeBodyPart);

			// second part is attachment
			if (null != attachment) {
				MimeBodyPart attachmentMimeBodyPart = new MimeBodyPart();
				DataSource dataSource = new ByteArrayDataSource(attachment,
						attachmentMimetype);
				attachmentMimeBodyPart.setDataHandler(new DataHandler(
						dataSource));
				multipart.addBodyPart(attachmentMimeBodyPart);
			}

			mimeMessage.setContent(multipart);
			Transport.send(mimeMessage);
		} catch (MessagingException e) {
			throw new RuntimeException("send failed, exception: "
					+ e.getMessage(), e);
		}
	}
}
