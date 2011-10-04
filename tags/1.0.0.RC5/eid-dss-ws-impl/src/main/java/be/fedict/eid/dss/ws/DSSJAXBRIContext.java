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

package be.fedict.eid.dss.ws;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.Binder;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Validator;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Node;

import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.bind.api.JAXBRIContext;
import com.sun.xml.bind.api.RawAccessor;
import com.sun.xml.bind.api.TypeReference;
import com.sun.xml.bind.v2.model.runtime.RuntimeTypeInfoSet;

public class DSSJAXBRIContext extends JAXBRIContext {

	private static final Log LOG = LogFactory.getLog(DSSJAXBRIContext.class);

	private final JAXBRIContext jaxbRiContext;

	public DSSJAXBRIContext(JAXBRIContext jaxbRiContext) {
		LOG.debug("constructor");
		this.jaxbRiContext = jaxbRiContext;
	}

	@Override
	public Bridge createBridge(TypeReference typeRef) {
		LOG.debug("createBridge");
		if (null != typeRef) {
			LOG.debug("type reference: " + typeRef.tagName);
		}
		/*
		 * We could influence the XML namespace via the Bridge somehow.
		 */
		return this.jaxbRiContext.createBridge(typeRef);
	}

	@Override
	public BridgeContext createBridgeContext() {
		LOG.debug("createBridgeContext");
		return this.jaxbRiContext.createBridgeContext();
	}

	@Override
	public void generateEpisode(Result result) {
		LOG.debug("generateEpisode");
		this.jaxbRiContext.generateEpisode(result);
	}

	@Override
	public void generateSchema(SchemaOutputResolver resolver)
			throws IOException {
		LOG.debug("generateSchema");
		this.jaxbRiContext.generateSchema(resolver);
	}

	@Override
	public String getBuildId() {
		LOG.debug("getBuildId");
		return this.jaxbRiContext.getBuildId();
	}

	@Override
	public QName getElementName(Object obj) throws JAXBException {
		LOG.debug("getElementName");
		return this.jaxbRiContext.getElementName(obj);
	}

	@Override
	public QName getElementName(Class clazz) throws JAXBException {
		LOG.debug("getElementName");
		return this.jaxbRiContext.getElementName(clazz);
	}

	@Override
	public <B, V> RawAccessor<B, V> getElementPropertyAccessor(Class<B> arg0,
			String arg1, String arg2) throws JAXBException {
		LOG.debug("getElementPropertyAccessor");
		return this.jaxbRiContext.getElementPropertyAccessor(arg0, arg1, arg2);
	}

	@Override
	public List<String> getKnownNamespaceURIs() {
		LOG.debug("getKnownNamespaceURIs");
		return this.jaxbRiContext.getKnownNamespaceURIs();
	}

	@Override
	public RuntimeTypeInfoSet getRuntimeTypeInfoSet() {
		LOG.debug("getRuntimeTypeInfoSet");
		return this.jaxbRiContext.getRuntimeTypeInfoSet();
	}

	@Override
	public QName getTypeName(TypeReference arg0) {
		LOG.debug("getTypeName");
		return this.jaxbRiContext.getTypeName(arg0);
	}

	@Override
	public boolean hasSwaRef() {
		LOG.debug("hasSwaRef");
		return this.jaxbRiContext.hasSwaRef();
	}

	@Override
	public Marshaller createMarshaller() throws JAXBException {
		LOG.debug("createMarshaller");
		Marshaller marshaller = this.jaxbRiContext.createMarshaller();
		marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",
				new DSSNamespacePrefixMapper());
		return marshaller;
	}

	@Override
	public Unmarshaller createUnmarshaller() throws JAXBException {
		LOG.debug("createUnmarshaller");
		return this.jaxbRiContext.createUnmarshaller();
	}

	@Override
	public Validator createValidator() throws JAXBException {
		LOG.debug("createValidator");
		return this.jaxbRiContext.createValidator();
	}

	@Override
	public Binder<Node> createBinder() {
		LOG.debug("createBinder");
		return this.jaxbRiContext.createBinder();
	}

	@Override
	public <T> Binder<T> createBinder(Class<T> domType) {
		LOG.debug("createBinder");
		return this.jaxbRiContext.createBinder(domType);
	}

	@Override
	public JAXBIntrospector createJAXBIntrospector() {
		LOG.debug("createJAXBIntrospector");
		return this.jaxbRiContext.createJAXBIntrospector();
	}
}
