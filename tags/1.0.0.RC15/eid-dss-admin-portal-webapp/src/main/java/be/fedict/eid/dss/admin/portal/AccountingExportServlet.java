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

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.List;

import javax.ejb.EJB;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.security.Identity;

import be.fedict.eid.dss.entity.AccountingEntity;
import be.fedict.eid.dss.model.AccountingService;

/**
 * Accounting export servlet. Exports the accounting data to CSV format.
 * 
 * @see http://tools.ietf.org/html/rfc4180
 * @author Frank Cornelis
 * 
 */
public class AccountingExportServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(AccountingExportServlet.class);

	@EJB
	private AccountingService accountingService;

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		LOG.debug("doGet");

		HttpSession httpSession = request.getSession();
		Identity identity = (Identity) httpSession
				.getAttribute("org.jboss.seam.security.identity");
		if (false == identity.hasRole("admin")) {
			response.sendError(HttpURLConnection.HTTP_FORBIDDEN,
					"no admin role");
			return;
		}

		response.setContentType("text/csv");
		PrintWriter printWriter = response.getWriter();
		List<AccountingEntity> accountingEntities = this.accountingService
				.listAll();
		for (AccountingEntity accountingEntity : accountingEntities) {
			printWriter.print("\"");
			printWriter.print(accountingEntity.getDomain());
			printWriter.print("\",\"");
			printWriter.print(accountingEntity.getRequests());
			printWriter.println("\"");
		}
	}
}
