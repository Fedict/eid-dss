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

package test.unit.be.fedict.eid.dss.webapp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.testing.ServletTester;

import be.fedict.eid.dss.webapp.SignatureRequestProcessorServlet;

public class SignatureRequestProcessorServletTest {

	private static final Log LOG = LogFactory
			.getLog(SignatureRequestProcessorServletTest.class);

	private ServletTester servletTester;

	private String location;

	@Before
	public void setUp() throws Exception {
		this.servletTester = new ServletTester();
		ServletHolder servletHolder = this.servletTester.addServlet(
				SignatureRequestProcessorServlet.class, "/");
		servletHolder.setInitParameter("NextPage", "next-page.html");
		servletHolder.setInitParameter("FinishPage", "finish-page.html");
		this.servletTester.start();
		this.location = this.servletTester.createSocketConnector(true);
	}

	@After
	public void tearDown() throws Exception {
		this.servletTester.stop();
	}

	@Test
	public void testGetMethod() throws Exception {
		// setup
		LOG.debug("URL: " + this.location);
		HttpClient httpClient = new HttpClient();
		GetMethod getMethod = new GetMethod(this.location);

		// operate
		int result = httpClient.executeMethod(getMethod);

		// verify
		assertEquals(HttpServletResponse.SC_OK, result);
		String responseBody = getMethod.getResponseBodyAsString();
		LOG.debug("Response body: " + responseBody);
	}

	@Test
	public void testMissingSignatureRequest() throws Exception {
		// setup
		LOG.debug("URL: " + this.location);
		HttpClient httpClient = new HttpClient();
		PostMethod postMethod = new PostMethod(this.location);

		// operate
		int result = httpClient.executeMethod(postMethod);

		// verify
		assertEquals(HttpServletResponse.SC_BAD_REQUEST, result);
		String responseBody = postMethod.getResponseBodyAsString();
		LOG.debug("Response body: " + responseBody);
	}

	@Test
	public void testSignatureRequestNotXML() throws Exception {
		// setup
		LOG.debug("URL: " + this.location);
		HttpClient httpClient = new HttpClient();
		PostMethod postMethod = new PostMethod(this.location);
		NameValuePair[] parametersBody = new NameValuePair[] { new NameValuePair(
				"SignatureRequest", "foobar") };
		postMethod.setRequestBody(parametersBody);

		// operate
		int result = httpClient.executeMethod(postMethod);

		// verify
		assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, result);
		String responseBody = postMethod.getResponseBodyAsString();
		LOG.debug("Response body: " + responseBody);
		String location = postMethod.getResponseHeader("Location").getValue();
		LOG.debug("location: " + location);
		assertTrue(location.endsWith("/finish-page.html"));
	}

	@Test
	public void testPost() throws Exception {
		// setup
		LOG.debug("URL: " + this.location);
		HttpClient httpClient = new HttpClient();
		PostMethod postMethod = new PostMethod(this.location);
		NameValuePair[] parametersBody = new NameValuePair[] { new NameValuePair(
				"SignatureRequest", new String(Base64
						.encode("<hello>world</hello>".getBytes()))) };
		postMethod.setRequestBody(parametersBody);

		// operate
		int result = httpClient.executeMethod(postMethod);

		// verify
		assertEquals(HttpServletResponse.SC_MOVED_TEMPORARILY, result);
		String responseBody = postMethod.getResponseBodyAsString();
		LOG.debug("Response body: " + responseBody);
		String location = postMethod.getResponseHeader("Location").getValue();
		LOG.debug("location: " + location);
		assertTrue(location.endsWith("/next-page.html"));
	}
}
