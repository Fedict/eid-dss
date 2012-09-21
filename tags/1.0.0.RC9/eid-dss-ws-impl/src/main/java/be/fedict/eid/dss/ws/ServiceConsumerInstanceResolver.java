/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2009-2010 FedICT.
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

import java.lang.reflect.Field;

import javax.ejb.EJB;
import javax.servlet.ServletContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.model.DocumentService;
import be.fedict.eid.dss.model.SignatureVerificationService;

import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.server.AbstractMultiInstanceResolver;

/**
 * JAX-WS RI Instance Resolver implementation to inject services into JAX-WS
 * endpoints.
 * 
 * @param <T>
 * @author Frank Cornelis
 */
public class ServiceConsumerInstanceResolver<T> extends
		AbstractMultiInstanceResolver<T> {

	private static final Log LOG = LogFactory
			.getLog(ServiceConsumerInstanceResolver.class);

	public ServiceConsumerInstanceResolver(Class<T> clazz) {
		super(clazz);
	}

	@Override
	public T resolve(Packet request) {
		T endpoint = create();

		ServletContext servletContext = (ServletContext) request
				.get(MessageContext.SERVLET_CONTEXT);

		SignatureVerificationService signatureVerificationService = ServiceConsumerServletContextListener
				.getSignatureVerificationService(servletContext);

		DocumentService documentService = ServiceConsumerServletContextListener
				.getDocumentService(servletContext);

		injectServices(endpoint, signatureVerificationService, documentService);

		return endpoint;
	}

	private void injectServices(T endpoint,
			SignatureVerificationService signatureVerificationService,
			DocumentService documentService) {

		LOG.debug("injecting services into JAX-WS endpoint...");
		Field[] fields = endpoint.getClass().getDeclaredFields();
		for (Field field : fields) {

			EJB ejbAnnotation = field.getAnnotation(EJB.class);
			if (null == ejbAnnotation) {
				continue;
			}
			if (field.getType().equals(SignatureVerificationService.class)) {
				field.setAccessible(true);
				try {
					field.set(endpoint, signatureVerificationService);
				} catch (Exception e) {
					throw new RuntimeException("injection error: "
							+ e.getMessage(), e);
				}
			} else if (field.getType().equals(DocumentService.class)) {
				field.setAccessible(true);
				try {
					field.set(endpoint, documentService);
				} catch (Exception e) {
					throw new RuntimeException("injection error: "
							+ e.getMessage(), e);
				}
			}

		}
	}
}
