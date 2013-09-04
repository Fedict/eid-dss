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

package be.fedict.eid.dss.webapp;

import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.web.Session;

/**
 * Via this JSF phase listener we clean-up the HTTP session when the eID DSS
 * hits the protocol-response-post.xhtml page.
 * 
 * @author Frank Cornelis
 * 
 */
public class SessionCleanupPhaseListener implements PhaseListener {

	private static final Log LOG = LogFactory
			.getLog(SessionCleanupPhaseListener.class);

	private static final long serialVersionUID = 1L;

	public void afterPhase(PhaseEvent event) {
		FacesContext facesContext = event.getFacesContext();
		UIViewRoot viewRoot = facesContext.getViewRoot();
		String viewId = viewRoot.getViewId();
		LOG.debug("view ID: " + viewId);
		if ("/protocol-response-post.xhtml".equals(viewId)) {
			LOG.debug("invalidating the HTTP session");
			Session seamSession = Session.getInstance();
			seamSession.invalidate();
		}
	}

	public void beforePhase(PhaseEvent event) {
	}

	public PhaseId getPhaseId() {
		return PhaseId.RENDER_RESPONSE;
	}
}
