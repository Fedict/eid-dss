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

package be.fedict.eid.dss.model.bean;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import be.fedict.eid.dss.model.ServicesManager;
import be.fedict.eid.dss.model.SignatureVerificationService;
import be.fedict.eid.dss.model.exception.DocumentFormatException;
import be.fedict.eid.dss.model.exception.InvalidSignatureException;
import be.fedict.eid.dss.spi.DSSDocumentService;
import be.fedict.eid.dss.spi.SignatureInfo;

@Stateless
public class SignatureVerificationServiceBean implements
		SignatureVerificationService {

	private static final Log LOG = LogFactory
			.getLog(SignatureVerificationServiceBean.class);

	@EJB
	private ServicesManager servicesManager;

	public List<SignatureInfo> verify(byte[] data, String mimeType,
			byte[] originalData) throws DocumentFormatException,
			InvalidSignatureException {
		LOG.debug("content type: " + mimeType);
		DSSDocumentService documentService = this.servicesManager
				.getDocumentService(mimeType);
		if (null == documentService) {
			LOG.error("no document service for content type: " + mimeType);
			throw new DocumentFormatException();
		}

		try {
			documentService.checkIncomingDocument(data);
		} catch (Exception e) {
			LOG.error("document check error: " + e.getMessage(), e);
			throw new DocumentFormatException();
		}

		List<SignatureInfo> signatureInfos;
		try {
			signatureInfos = documentService.verifySignatures(data,
					originalData);
		} catch (Exception e) {
			LOG.error("error verifying signatures: " + e.getMessage(), e);
			throw new InvalidSignatureException();
		}
		return signatureInfos;
	}
}