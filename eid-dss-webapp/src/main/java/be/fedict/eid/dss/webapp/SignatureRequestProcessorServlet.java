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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import be.fedict.eid.dss.model.DocumentRepository;
import be.fedict.eid.dss.model.SignatureStatus;

public class SignatureRequestProcessorServlet extends HttpServlet {

	private static final long serialVersionUID = 1L;

	public static final String SIGNATURE_REQUEST_PARAMETER = "SignatureRequest";

	public static final String TARGET_PARAMETER = "target";

	public static final String LANGUAGE_PARAMETER = "language";

	public static final String LANGUAGE_SESSION_ATTRIBUTE = SignatureRequestProcessorServlet.class
			.getName() + ".language";

	public static final String NEXT_PAGE_INIT_PARAM = "NextPage";

	public static final String FINISH_PAGE_INIT_PARAM = "FinishPage";

	private static final Log LOG = LogFactory
			.getLog(SignatureRequestProcessorServlet.class);

	private String nextPage;

	private String finishPage;

	@Override
	public void init(ServletConfig config) throws ServletException {
		LOG.debug("init");
		this.nextPage = config.getInitParameter(NEXT_PAGE_INIT_PARAM);
		if (null == this.nextPage) {
			throw new ServletException("missing init-param: "
					+ NEXT_PAGE_INIT_PARAM);
		}
		LOG.debug("next page: " + this.nextPage);
		this.finishPage = config.getInitParameter(FINISH_PAGE_INIT_PARAM);
		if (null == this.finishPage) {
			throw new ServletException("missing init-param: "
					+ FINISH_PAGE_INIT_PARAM);
		}
		LOG.debug("finish page: " + this.finishPage);
	}

	@Override
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		LOG.debug("doGet");
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("<head><title>eID DSS Signature Request Processor</title></head>");
		out.println("<body>");
		out.println("<h1>eID DSS Signature Request Processor</h1>");
		out.println("<p>The Signature Processor should not be accessed directly.</p>");
		out.println("</body></html>");
		out.close();
	}

	@Override
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		LOG.debug("doPost");
		String target = request.getParameter(TARGET_PARAMETER);
		HttpSession httpSession = request.getSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);

		/*
		 * Make sure we don't use previous results.
		 */
		documentRepository.reset();

		if (null != target) {
			LOG.debug("target: " + target);
			documentRepository.setTarget(target);
		}

		String signatureRequest = request
				.getParameter(SIGNATURE_REQUEST_PARAMETER);
		if (null == signatureRequest) {
			String msg = SIGNATURE_REQUEST_PARAMETER + " parameter not present";
			LOG.error(msg);
			showErrorPage(msg, response);
			return;
		}
		byte[] decodedSignatureRequest = Base64.decode(signatureRequest);
		LOG.debug("decoded signature request: "
				+ new String(decodedSignatureRequest));
		try {
			loadDocument(new ByteArrayInputStream(decodedSignatureRequest));
		} catch (Exception e) {
			documentRepository.setSignatureStatus(SignatureStatus.FILE_FORMAT);
			response.sendRedirect(this.finishPage);
			return;
		}
		documentRepository.setDocument(new String(decodedSignatureRequest));

		String language = request.getParameter(LANGUAGE_PARAMETER);
		if (null != language) {
			setLanguage(language, httpSession);
		}

		response.sendRedirect(this.nextPage);
	}

	private void setLanguage(String language, HttpSession httpSession) {
		httpSession.setAttribute(LANGUAGE_SESSION_ATTRIBUTE, language);
	}

	private Document loadDocument(InputStream documentInputStream)
			throws ParserConfigurationException, SAXException, IOException {
		InputSource inputSource = new InputSource(documentInputStream);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		Document document = documentBuilder.parse(inputSource);
		return document;
	}

	private void showErrorPage(String message, HttpServletResponse response)
			throws IOException {
		response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
		response.setContentType("text/html");
		PrintWriter out = response.getWriter();
		out.println("<html>");
		out.println("<head><title>eID DSS Signature Request Processor</title></head>");
		out.println("<body>");
		out.println("<h1>eID DSS Signature Request Processor</h1>");
		out.println("<p>ERROR: " + message + "</p>");
		out.println("</body></html>");
		out.close();
	}
}
