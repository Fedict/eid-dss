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

package be.fedict.eid.dss.sp.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DownloadServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory.getLog(DownloadServlet.class);

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		LOG.debug("doGet");
		HttpSession httpSession = request.getSession();
		byte[] document = (byte[]) httpSession.getAttribute("document");

		response.setHeader("Cache-Control",
				"no-cache, no-store, must-revalidate, max-age=-1"); // http 1.1
		if (!request.getScheme().equals("https")) {
			// else the download fails in IE
			response.setHeader("Pragma", "no-cache"); // http 1.0
		} else {
			response.setHeader("Pragma", "public");
		}
		response.setDateHeader("Expires", -1);
		response.setContentLength(document.length);

		String contentType = (String) httpSession.getAttribute("ContentType");
		LOG.debug("content-type: " + contentType);
		response.setContentType(contentType);
		response.setContentLength(document.length);
		ServletOutputStream out = response.getOutputStream();
		out.write(document);
		out.flush();
	}

}
