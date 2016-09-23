/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009 FedICT.
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

package be.fedict.eid.dss.portal.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import be.fedict.eid.dss.portal.control.state.SigningModel;
import be.fedict.eid.dss.portal.control.state.SigningModelRepository;

public class DownloadServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		SigningModel signingModel = SigningModelRepository.get(request.getSession());
		byte[] document = signingModel.getDocument();
		String contentType = signingModel.getContentType();

		setHeaders(request, response, contentType, document.length);
		writeDocument(response, document);
	}

	private void writeDocument(HttpServletResponse response, byte[] document) throws IOException {
		ServletOutputStream out = response.getOutputStream();
		out.write(document);
		out.flush();
	}

	private void setHeaders(HttpServletRequest request, HttpServletResponse response, String contentType, int contentLength) {
		response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate, max-age=-1");
		if (!request.getScheme().equals("https")) {
			response.setHeader("Pragma", "no-cache");
		} else {
			response.setHeader("Pragma", "public");
		}
		response.setDateHeader("Expires", -1);
		response.setContentLength(contentLength);
		response.setContentType(contentType);
	}
}
