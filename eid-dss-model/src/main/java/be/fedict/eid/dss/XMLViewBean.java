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

package be.fedict.eid.dss;

import javax.ejb.Stateless;
import javax.servlet.http.HttpSession;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Name;

import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;

@Stateless
@Name("xmlView")
@LocalBinding(jndiBinding = "fedict/eid/dss/XMLViewBean")
public class XMLViewBean implements XMLView {

	public String cancel() {
		HttpSession httpSession = HttpSessionTemporaryDataStorage
				.getHttpSession();
		DocumentRepository documentRepository = new DocumentRepository(
				httpSession);
		documentRepository.setSignatureStatus(SignatureStatus.USER_CANCELLED);
		return "cancel";
	}
}
