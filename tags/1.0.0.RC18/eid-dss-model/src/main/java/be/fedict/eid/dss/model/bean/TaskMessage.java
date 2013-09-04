/*
 * eID Digital Signature Service Project.
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

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueSession;

/**
 * Basic interface for a task message.
 * 
 * @author Frank Cornelis
 * 
 */
public interface TaskMessage {

	/**
	 * Creates a new JMS message using the given queue session and the internal
	 * state of the task message object.
	 * 
	 * @param queueSession
	 * @return
	 * @throws JMSException
	 */
	Message getMessage(QueueSession queueSession) throws JMSException;
}
