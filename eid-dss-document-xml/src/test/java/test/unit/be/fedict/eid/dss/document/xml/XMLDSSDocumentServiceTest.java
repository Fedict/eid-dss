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

package test.unit.be.fedict.eid.dss.document.xml;

import org.apache.commons.io.IOUtils;
import org.easymock.EasyMock;
import org.junit.Test;

import be.fedict.eid.dss.document.xml.XMLDSSDocumentService;
import be.fedict.eid.dss.spi.DSSDocumentContext;

public class XMLDSSDocumentServiceTest {

	@Test
	public void testCheckIncomingDocumentWithoutNamespace() throws Exception {
		// setup
		XMLDSSDocumentService testedInstance = new XMLDSSDocumentService();
		byte[] document = "<test>hello world</test>".getBytes();

		// operate
		testedInstance.init(null, null);
		testedInstance.checkIncomingDocument(document);
	}

	@Test
	public void testCheckIncomingDocumentUnknownNamespace() throws Exception {
		// setup
		XMLDSSDocumentService testedInstance = new XMLDSSDocumentService();
		byte[] document = "<test xmlns=\"urn:test\">hello world</test>"
				.getBytes();
		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);

		// expectations
		EasyMock.expect(mockContext.getXmlSchema("urn:test")).andReturn(null);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(null, mockContext);
		testedInstance.checkIncomingDocument(document);

		// verify
		EasyMock.verify(mockContext);
	}

	@Test
	public void testCheckIncomingDocumentWithNamespaceChecking()
			throws Exception {
		// setup
		XMLDSSDocumentService testedInstance = new XMLDSSDocumentService();
		byte[] document = "<test xmlns=\"urn:test\">hello world</test>"
				.getBytes();
		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);

		byte[] xsd = IOUtils.toByteArray(XMLDSSDocumentServiceTest.class
				.getResourceAsStream("/test.xsd"));

		// expectations
		EasyMock.expect(mockContext.getXmlSchema("urn:test")).andReturn(xsd);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(null, mockContext);
		testedInstance.checkIncomingDocument(document);

		// verify
		EasyMock.verify(mockContext);
	}

	@Test
	public void testCheckIncomingDocumentWithNamespaceCheckingWithImporting()
			throws Exception {
		// setup
		XMLDSSDocumentService testedInstance = new XMLDSSDocumentService();
		byte[] document = ("<test2 xmlns=\"urn:test2\" xmlns:test=\"urn:test\">"
				+ "<test:test>hello world</test:test></test2>").getBytes();
		DSSDocumentContext mockContext = EasyMock
				.createMock(DSSDocumentContext.class);

		byte[] xsd2 = IOUtils.toByteArray(XMLDSSDocumentServiceTest.class
				.getResourceAsStream("/test-import.xsd"));
		byte[] xsd = IOUtils.toByteArray(XMLDSSDocumentServiceTest.class
				.getResourceAsStream("/test.xsd"));

		// expectations
		EasyMock.expect(mockContext.getXmlSchema("urn:test2")).andReturn(xsd2);
		EasyMock.expect(mockContext.getXmlSchema("urn:test")).andReturn(xsd);

		// prepare
		EasyMock.replay(mockContext);

		// operate
		testedInstance.init(null, mockContext);
		testedInstance.checkIncomingDocument(document);

		// verify
		EasyMock.verify(mockContext);
	}
}
