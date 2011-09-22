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

import java.util.List;

import javax.xml.bind.JAXBException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.xml.bind.api.JAXBRIContext;
import com.sun.xml.bind.api.TypeReference;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.developer.JAXBContextFactory;

/**
 * JAXB Context factory for JAX-WS RI. We added this as this delivers us clean
 * SOAP XML namespace prefixes and thus eases the job of documenting the
 * protocol.
 * 
 * @author Frank Cornelis
 * 
 */
public class DSSJAXBContextFactory implements JAXBContextFactory {

	private static final Log LOG = LogFactory
			.getLog(DSSJAXBContextFactory.class);

	public JAXBRIContext createJAXBContext(SEIModel seiModel,
			List<Class> classList, List<TypeReference> typeReferences)
			throws JAXBException {
		LOG.debug("createJAXBContext");
		JAXBRIContext jaxbRiContext = JAXBRIContext.newInstance(
				classList.toArray(new Class[classList.size()]), typeReferences,
				null, seiModel.getTargetNamespace(), false, null);
		DSSJAXBRIContext dssJaxbRiContext = new DSSJAXBRIContext(jaxbRiContext);
		return dssJaxbRiContext;
	}
}
