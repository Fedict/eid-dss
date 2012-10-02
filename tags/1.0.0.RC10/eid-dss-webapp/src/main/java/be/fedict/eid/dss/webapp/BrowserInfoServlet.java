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

package be.fedict.eid.dss.webapp;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import be.fedict.eid.dss.spi.MimeType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BrowserInfoServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory.getLog(BrowserInfoServlet.class);

	private static final String PLUGINS_SESSION_ATTRIBUTE = BrowserInfoServlet.class
			.getName() + ".Plugins";
	private static final String MIMETYPES_SESSION_ATTRIBUTE = BrowserInfoServlet.class
			.getName() + ".MimeTypes";

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		LOG.debug("doPost");

		// parse plugins
		String pluginList = request.getParameter("plugins");
		if (null != pluginList && !pluginList.isEmpty()) {
			LOG.debug("pluginList: " + pluginList);
			String[] pluginarray = pluginList.split("\\|");
			List<String> plugins = Arrays.asList(pluginarray);
			request.getSession().setAttribute(PLUGINS_SESSION_ATTRIBUTE,
					plugins);
		}

		// parse mime types
		String mimeTypesList = request.getParameter("mimeTypes");
		if (null != mimeTypesList && !mimeTypesList.isEmpty()) {
			LOG.debug("mimeTypesList: " + mimeTypesList);
			List<MimeType> mimeTypes = new LinkedList<MimeType>();
			for (String mimeType : mimeTypesList.split("\\|")) {
				String[] mimeTypeInfo = mimeType.split(",");
				if (mimeTypeInfo.length != 2) {
					LOG.error("Unable to parse mimetype: " + mimeType);
					continue;
				}
				mimeTypes.add(new MimeType(mimeTypeInfo[0], mimeTypeInfo[1]));
			}
			request.getSession().setAttribute(MIMETYPES_SESSION_ATTRIBUTE,
					mimeTypes);
		}

		response.sendRedirect("./view.seam");
	}

	@SuppressWarnings("unchecked")
	public static List<String> getPlugins(HttpSession httpSession) {
		List<String> plugins = (List<String>) httpSession
				.getAttribute(PLUGINS_SESSION_ATTRIBUTE);
		if (null == plugins) {
			plugins = new LinkedList<String>();
		}
		return plugins;
	}

	@SuppressWarnings("unchecked")
	public static List<MimeType> getMimeTypes(HttpSession httpSession) {
		List<MimeType> mimeTypes = (List<MimeType>) httpSession
				.getAttribute(MIMETYPES_SESSION_ATTRIBUTE);
		if (null == mimeTypes) {
			mimeTypes = new LinkedList<MimeType>();
		}
		return mimeTypes;
	}
}
