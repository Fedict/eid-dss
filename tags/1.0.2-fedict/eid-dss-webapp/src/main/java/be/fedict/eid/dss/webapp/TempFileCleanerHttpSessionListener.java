/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009-2012 FedICT.
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

package be.fedict.eid.dss.webapp;

import java.io.File;
import java.util.Set;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.model.bean.ModelDSSDocumentContext;

/**
 * Handles the cleanup of temporary files used by the different document
 * signature modules.
 * 
 * @author Frank Cornelis
 * 
 */
public class TempFileCleanerHttpSessionListener implements HttpSessionListener {

	private static final Log LOG = LogFactory
			.getLog(TempFileCleanerHttpSessionListener.class);

	@Override
	public void sessionCreated(HttpSessionEvent event) {
		// empty
	}

	@Override
	public void sessionDestroyed(HttpSessionEvent event) {
		LOG.debug("sessionDestroyed");
		HttpSession httpSession = event.getSession();
		Set<String> tmpFileSet = (Set<String>) httpSession
				.getAttribute(ModelDSSDocumentContext.TMP_FILE_SET_SESSION_ATTRIBUTE);
		if (null == tmpFileSet) {
			LOG.debug("no temp file set in HTTP session present");
			return;
		}
		for (String tmpFilename : tmpFileSet) {
			File tmpFile = new File(tmpFilename);
			if (false == tmpFile.exists()) {
				LOG.debug("tmp file already removed: " + tmpFilename);
				continue;
			}
			if (tmpFile.delete()) {
				LOG.debug("tmp file successfully deleted: " + tmpFilename);
			} else {
				LOG.warn("tmp file could not be removed: " + tmpFilename);
			}
		}
	}
}
