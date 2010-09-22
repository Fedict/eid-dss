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

package be.fedict.eid.dss.document.xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;

import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.signer.time.TimeStampServiceValidator;
import be.fedict.eid.applet.service.spi.SignatureService;
import be.fedict.eid.dss.spi.DSSDocumentContext;
import be.fedict.eid.dss.spi.DSSDocumentService;
import be.fedict.eid.dss.spi.DocumentVisualization;

/**
 * Document Service implementation for XML documents.
 * 
 * @author Frank Cornelis
 * 
 */
public class XMLDSSDocumentService implements DSSDocumentService {

	private static final long serialVersionUID = 1L;

	private static final Log LOG = LogFactory
			.getLog(XMLDSSDocumentService.class);

	private DocumentBuilder documentBuilder;

	private DSSDocumentContext context;

	private TransformerFactory transformerFactory;

	public void checkIncomingDocument(byte[] document) throws Exception {
		LOG.debug("checking incoming document");
		ByteArrayInputStream documentInputStream = new ByteArrayInputStream(
				document);
		Document dom = this.documentBuilder.parse(documentInputStream);

		String namespace = dom.getDocumentElement().getNamespaceURI();
		if (null == namespace) {
			LOG.debug("no namespace defined");
			return;
		}

		byte[] xsd = this.context.getXmlSchema(namespace);
		if (null == xsd) {
			LOG.debug("no XML schema available for namespace: " + namespace);
			return;
		}

		LOG.debug("validating against XML schema: " + namespace);
		SchemaFactory schemaFactory = SchemaFactory
				.newInstance("http://www.w3.org/2001/XMLSchema");
		schemaFactory
				.setResourceResolver(new SignatureServiceLSResourceResolver(this.context));
		StreamSource schemaSource = new StreamSource(new ByteArrayInputStream(
				xsd));
		Schema schema = schemaFactory.newSchema(schemaSource);
		Validator validator = schema.newValidator();
		DOMSource domSource = new DOMSource(dom);
		validator.validate(domSource);
	}

	public void init(ServletContext servletContext, DSSDocumentContext context)
			throws Exception {
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory
				.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		this.documentBuilder = documentBuilderFactory.newDocumentBuilder();
		this.context = context;
		this.transformerFactory = TransformerFactory.newInstance();
	}

	public SignatureService getSignatureService(
			InputStream documentInputStream, TimeStampService timeStampService,
			TimeStampServiceValidator timeStampServiceValidator,
			RevocationDataService revocationDataService,
			SignatureFacet signatureFacet, OutputStream documentOutputStream) {
		return new XMLSignatureService(timeStampServiceValidator,
				revocationDataService, signatureFacet, documentInputStream,
				documentOutputStream, timeStampService);
	}

	public DocumentVisualization visualizeDocument(byte[] document,
			String language) throws Exception {
		// per default we do nothing
		byte[] browserData = document;
		String browserContentType = "text/xml";

		ByteArrayInputStream documentInputStream = new ByteArrayInputStream(
				document);
		Document dom = this.documentBuilder.parse(documentInputStream);
		String namespace = dom.getDocumentElement().getNamespaceURI();
		if (null != namespace) {
			LOG.debug("document namespace: " + namespace);
			byte[] xsl = this.context.getXmlStyleSheet(namespace);
			if (null != xsl) {
				LOG.debug("XML style sheet present");
				browserContentType = "text/html";
				Transformer transformer = this.transformerFactory
						.newTransformer(new StreamSource(
								new ByteArrayInputStream(xsl)));
				if (null != language) {
					transformer.setParameter("language", language);
				}
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				transformer.transform(new DOMSource(dom), new StreamResult(
						outputStream));
				browserData = outputStream.toByteArray();
			}
		}

		DocumentVisualization documentVisualization = new DocumentVisualization(
				browserContentType, browserData);
		return documentVisualization;
	}
}
