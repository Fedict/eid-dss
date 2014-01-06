/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2010-2011 FedICT.
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

package be.fedict.eid.dss.document.asic;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import be.fedict.eid.applet.service.signer.DigestAlgo;
import be.fedict.eid.applet.service.signer.HttpSessionTemporaryDataStorage;
import be.fedict.eid.applet.service.signer.SignatureFacet;
import be.fedict.eid.applet.service.signer.asic.AbstractASiCSignatureService;
import be.fedict.eid.applet.service.signer.facets.RevocationDataService;
import be.fedict.eid.applet.service.signer.time.TimeStampService;
import be.fedict.eid.applet.service.spi.IdentityDTO;

/**
 * Associated Signature Container signature service implementation. Were we
 * simply bind against the HTTP session based temporary data storage.
 * 
 * @author Frank Cornelis
 * 
 */
public class ASiCSignatureService extends AbstractASiCSignatureService {

	public ASiCSignatureService(InputStream documentInputStream,
			DigestAlgo digestAlgo, RevocationDataService revocationDataService,
			TimeStampService timeStampService, String claimedRole,
			IdentityDTO identity, byte[] photo,
			OutputStream documentOutputStream, SignatureFacet signatureFacet)
			throws IOException {
		super(documentInputStream, digestAlgo, revocationDataService,
				timeStampService, claimedRole, identity, photo,
				new HttpSessionTemporaryDataStorage(), documentOutputStream);
		addSignatureFacet(signatureFacet);
	}
}
