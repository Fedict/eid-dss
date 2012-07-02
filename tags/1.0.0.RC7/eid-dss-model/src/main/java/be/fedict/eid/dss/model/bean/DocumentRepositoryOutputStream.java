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

package be.fedict.eid.dss.model.bean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.http.HttpSession;

import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;
import be.fedict.eid.dss.model.DocumentRepository;
import be.fedict.eid.dss.spi.SignatureStatus;

/**
 * An output stream that will eventually write back to the current document
 * repository.
 * 
 * @author Frank Cornelis
 * 
 */
public class DocumentRepositoryOutputStream extends ByteArrayOutputStream {

	@Override
	public void close() throws IOException {
		super.close();

		byte[] data = super.toByteArray();
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		documentRepository.setSignedDocument(data);
		documentRepository.setSignatureStatus(SignatureStatus.OK);
	}
}
