/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2011 FedICT.
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

import static be.fedict.eid.dss.model.bean.MailSignedDocumentTaskMessage.TASK_NAME;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueSession;

/**
 * JMS task message for mailing signed documents.
 * 
 * @author Frank Cornelis
 * 
 */
@Task(TASK_NAME)
class MailSignedDocumentTaskMessage implements TaskMessage {

	public static final String TASK_NAME = "MailSignedDocument";

	private final String email;

	private final String language;

	private final String mimetype;

	private final byte[] document;

	public MailSignedDocumentTaskMessage(String email, String language,
			String mimetype, byte[] document) {
		this.email = email;
		this.language = language;
		this.mimetype = mimetype;
		this.document = document;
	}

	public MailSignedDocumentTaskMessage(Message message) {
		if (false == message instanceof BytesMessage) {
			throw new RuntimeException("message should be BytesMessage");
		}
		BytesMessage bytesMessage = (BytesMessage) message;
		try {
			this.email = bytesMessage.getStringProperty("email");
			this.language = bytesMessage.getStringProperty("language");
			this.mimetype = bytesMessage.getStringProperty("mimetype");
			int documentLength = (int) bytesMessage.getBodyLength();
			this.document = new byte[documentLength];
			if (documentLength != bytesMessage.readBytes(this.document)) {
				throw new JMSException("could not read entire JMS message body");
			}
		} catch (JMSException e) {
			throw new RuntimeException("JMS error: " + e.getMessage(), e);
		}
	}

	public Message getMessage(QueueSession queueSession) throws JMSException {
		BytesMessage bytesMessage = queueSession.createBytesMessage();
		bytesMessage.setStringProperty("email", this.email);
		bytesMessage.setStringProperty("language", this.language);
		bytesMessage.setStringProperty("mimetype", this.mimetype);
		bytesMessage.writeBytes(this.document);
		return bytesMessage;
	}

	public String getEmail() {
		return this.email;
	}

	public String getLanguage() {
		return this.language;
	}

	public String getMimetype() {
		return this.mimetype;
	}

	public byte[] getDocument() {
		return this.document;
	}
}
