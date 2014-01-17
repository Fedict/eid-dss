/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010-2012 FedICT.
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

package be.fedict.eid.dss.admin.portal;

import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.application.ViewHandler;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.servlet.http.HttpSession;

/**
 * Session timeout JSF phase listener.
 * 
 * @author Frank Cornelis
 * 
 */
public class SessionTimeoutPhaseListener implements PhaseListener {

	private static final long serialVersionUID = 1L;

	@Override
	public void beforePhase(PhaseEvent event) {
		FacesContext facesContext = event.getFacesContext();
		ExternalContext externalContext = facesContext.getExternalContext();
		HttpSession httpSession = (HttpSession) externalContext
				.getSession(false);
		boolean newSession = (httpSession == null) || (httpSession.isNew());
		boolean postBack = !externalContext.getRequestParameterMap().isEmpty();
		boolean timedOut = postBack && newSession;
		if (timedOut) {
			Application application = facesContext.getApplication();
			ViewHandler viewHandler = application.getViewHandler();
			UIViewRoot view = viewHandler.createView(facesContext,
					"/main.xhtml");
			facesContext.setViewRoot(view);
			facesContext.renderResponse();
			try {
				viewHandler.renderView(facesContext, view);
				facesContext.responseComplete();
			} catch (Exception e) {
				throw new FacesException("Session timed out", e);
			}
		}
	}

	@Override
	public void afterPhase(PhaseEvent event) {
	}

	@Override
	public PhaseId getPhaseId() {
		return PhaseId.RESTORE_VIEW;
	}
}
