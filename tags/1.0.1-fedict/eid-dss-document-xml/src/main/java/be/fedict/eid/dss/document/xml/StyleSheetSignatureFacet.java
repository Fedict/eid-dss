/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2011 FedICT.
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

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.crypto.XMLStructure;
import javax.xml.crypto.dom.DOMStructure;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.crypto.dsig.Reference;
import javax.xml.crypto.dsig.Transform;
import javax.xml.crypto.dsig.XMLObject;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.spec.TransformParameterSpec;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import be.fedict.eid.applet.service.signer.DigestAlgo;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.dss.document.xml.jaxb.stylesheet.ObjectFactory;
import be.fedict.eid.dss.document.xml.jaxb.stylesheet.StyleSheetType;
import be.fedict.eid.dss.spi.DSSDocumentContext;

/**
 * XML stylesheet signature facet.
 * <p/>
 * Can be used to include the XML stylesheet used to visualize the XML document.
 * 
 * @author Frank Cornelis.
 * 
 */
public class StyleSheetSignatureFacet implements SignatureFacet {

	public static final String REFERENCE_TYPE = "be:fedict:eid:dss:stylesheet:1.0";

	private final ObjectFactory objectFactory;
	private final Marshaller marshaller;
	private final DSSDocumentContext documentContext;
	private final DigestAlgo digestAlgo;

	public StyleSheetSignatureFacet(DSSDocumentContext documentContext,
			DigestAlgo digestAlgo) {
		this.documentContext = documentContext;
		this.digestAlgo = digestAlgo;
		this.objectFactory = new ObjectFactory();
		try {
			JAXBContext jaxbContext = JAXBContext
					.newInstance(ObjectFactory.class);
			this.marshaller = jaxbContext.createMarshaller();
		} catch (JAXBException e) {
			throw new RuntimeException("JAXB error: " + e.getMessage(), e);
		}
	}

	@Override
	public void preSign(XMLSignatureFactory signatureFactory,
			Document document, String signatureId,
			List<X509Certificate> signingCertificateChain,
			List<Reference> references, List<XMLObject> objects)
			throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
		String namespace = document.getDocumentElement().getNamespaceURI();
		if (null == namespace) {
			return;
		}
		byte[] stylesheetData = this.documentContext
				.getXmlStyleSheet(namespace);
		if (null == stylesheetData) {
			return;
		}

		// JAXB element construction
		StyleSheetType styleSheet = this.objectFactory.createStyleSheetType();
		styleSheet.setValue(stylesheetData);
		String styleSheetId = "stylesheet-" + UUID.randomUUID().toString();
		styleSheet.setId(styleSheetId);

		// marshalling
		Node marshallNode = document.createElement("marshall-node");
		try {
			this.marshaller.marshal(
					this.objectFactory.createStyleSheet(styleSheet),
					marshallNode);
		} catch (JAXBException e) {
			throw new RuntimeException("JAXB error: " + e.getMessage(), e);
		}
		Node styleSheetNode = marshallNode.getFirstChild();
		
		// fix for xmlsec
		Element styleSheetElement = (Element) styleSheetNode;
		styleSheetElement.setIdAttribute("Id", true);

		// ds:Object
		List<XMLStructure> styleSheetObjectContent = new LinkedList<XMLStructure>();
		styleSheetObjectContent.add(new DOMStructure(styleSheetNode));
		XMLObject styleSheetObject = signatureFactory.newXMLObject(
				styleSheetObjectContent, null, null, null);
		objects.add(styleSheetObject);

		// ds:Reference
		DigestMethod digestMethod = signatureFactory.newDigestMethod(
				this.digestAlgo.getXmlAlgoId(), null);
		List<Transform> transforms = new LinkedList<Transform>();
		Transform exclusiveTransform = signatureFactory
				.newTransform(CanonicalizationMethod.INCLUSIVE,
						(TransformParameterSpec) null);
		transforms.add(exclusiveTransform);
		Reference reference = signatureFactory.newReference("#" + styleSheetId,
				digestMethod, transforms, REFERENCE_TYPE, null);
		references.add(reference);
	}

	@Override
	public void postSign(Element signatureElement,
			List<X509Certificate> signingCertificateChain) {
		// empty
	}
}
