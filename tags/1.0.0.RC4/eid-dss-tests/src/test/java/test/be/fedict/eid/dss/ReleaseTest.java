/*
 * eID Applet Project.
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

package test.be.fedict.eid.dss;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import com.sun.org.apache.xpath.internal.XPathAPI;

/**
 * We do our own release process since we're not really happy with the
 * maven-release-plugin.
 * 
 * @author Frank Cornelis
 * 
 */
public class ReleaseTest {

	private static final Log LOG = LogFactory.getLog(ReleaseTest.class);

	// 1.0.0-SNAPSHOT
	private static final String CURRENT_VERSION = "1.0.0.RC4";

	// 1.0.0.RC1
	private static final String NEW_VERSION = "1.0.0-SNAPSHOT";

	@Test
	public void testVersioning() throws Exception {
		Thread thread = Thread.currentThread();
		ClassLoader classLoader = thread.getContextClassLoader();
		String classResourceName = ReleaseTest.class.getName().replaceAll(
				"\\.", "\\/")
				+ ".class";
		URL classUrl = classLoader.getResource(classResourceName);
		LOG.debug("class URL: " + classUrl);
		URL baseUrl = new URL(
				classUrl.toString()
						.substring(
								0,
								classUrl.toString()
										.indexOf(
												"eid-dss-tests/target/test-classes/test/be/fedict/eid/dss/ReleaseTest.class")));
		LOG.debug("base URL: " + baseUrl);
		File baseDir = new File(baseUrl.toURI());
		List<File> pomFiles = new LinkedList<File>();
		getPomFiles(baseDir, pomFiles);
		LOG.debug("# pom.xml files: " + pomFiles.size());
		for (File pomFile : pomFiles) {
			LOG.debug("pom.xml: " + pomFile.getAbsolutePath());
			Document pomDocument = loadDocument(pomFile);

			Node projectVersionTextNode = XPathAPI
					.selectSingleNode(
							pomDocument.getDocumentElement(),
							"/:project[:groupId[contains(text(), 'be.fedict.eid-dss')]]/:version/text()",
							pomDocument.getDocumentElement());
			if (null != projectVersionTextNode) {
				assertEquals(CURRENT_VERSION,
						projectVersionTextNode.getNodeValue());
				projectVersionTextNode.setNodeValue(NEW_VERSION);
			}

			// parent POM version within POMs
			projectVersionTextNode = XPathAPI
					.selectSingleNode(
							pomDocument.getDocumentElement(),
							"/:project/:parent[:groupId[text() = 'be.fedict'] and :artifactId[contains(text(), 'eid-dss')]]/:version/text()",
							pomDocument.getDocumentElement());
			if (null != projectVersionTextNode) {
				assertEquals(CURRENT_VERSION,
						projectVersionTextNode.getNodeValue());
				projectVersionTextNode.setNodeValue(NEW_VERSION);
			}

			// parent POM version
			projectVersionTextNode = XPathAPI
					.selectSingleNode(
							pomDocument.getDocumentElement(),
							"/:project[:groupId[text() = 'be.fedict'] and :artifactId[contains(text(), 'eid-dss')]]/:version/text()",
							pomDocument.getDocumentElement());
			if (null != projectVersionTextNode) {
				assertEquals(CURRENT_VERSION,
						projectVersionTextNode.getNodeValue());
				projectVersionTextNode.setNodeValue(NEW_VERSION);
			}

			storeDocument(pomDocument, pomFile);
		}
	}

	private void getPomFiles(File dir, List<File> pomFiles) {
		File[] files = dir.listFiles();
		for (File file : files) {
			if ("pom.xml".equals(file.getName())) {
				pomFiles.add(file);
			}
			if (file.isDirectory()) {
				getPomFiles(file, pomFiles);
			}
		}
	}

	private Document loadDocument(File file)
			throws ParserConfigurationException, SAXException, IOException {
		FileInputStream documentInputStream = new FileInputStream(file);
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder documentBuilder = documentBuilderFactory
				.newDocumentBuilder();
		return documentBuilder.parse(documentInputStream);
	}

	private void storeDocument(Document document, File file)
			throws FileNotFoundException, TransformerException {
		OutputStream outputStream = new FileOutputStream(file);
		Source source = new DOMSource(document);
		Result result = new StreamResult(outputStream);
		TransformerFactory transformerFactory = TransformerFactory
				.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		transformer.transform(source, result);
	}
}
