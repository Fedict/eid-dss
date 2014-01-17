/*
 * eID Digital Signature Service Project.
 * Copyright (C) 2011 Frank Cornelis.
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

package be.fedict.eid.dss.document.zip;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.utils.resolver.ResourceResolverException;
import org.apache.xml.security.utils.resolver.ResourceResolverSpi;
import org.w3c.dom.Attr;

public class ZIPResourceResolver extends ResourceResolverSpi {

	private static final Log LOG = LogFactory.getLog(ZIPResourceResolver.class);

	private final byte[] document;

	public ZIPResourceResolver(byte[] document) {
		this.document = document;
	}

	@Override
	public boolean engineCanResolve(Attr uri, String baseUri) {
		LOG.debug("engineCanResolve: " + uri.getValue());
		try {
			if (null != findZIPEntry(uri)) {
				return true;
			}
		} catch (IOException e) {
			LOG.error("IO error: " + e.getMessage(), e);
			return false;
		}
		/*
		 * We claim we can resolve em all.
		 */
		return true;
	}

	@Override
	public XMLSignatureInput engineResolve(Attr uri, String baseUri)
			throws ResourceResolverException {
		LOG.debug("engineResolve: " + uri.getValue());
		InputStream inputStream;
		try {
			inputStream = findZIPEntry(uri);
		} catch (IOException e) {
			throw new ResourceResolverException("IO error: " + e.getMessage(),
					e, uri, baseUri);
		}
		XMLSignatureInput signatureInput = new XMLSignatureInput(inputStream);
		return signatureInput;
	}

	private InputStream findZIPEntry(Attr dsReferenceUri) throws IOException {
		String entryName = dsReferenceUri.getValue();
		ZipInputStream zipInputStream = new ZipInputStream(
				new ByteArrayInputStream(this.document));
		ZipEntry zipEntry;
		while (null != (zipEntry = zipInputStream.getNextEntry())) {
			if (entryName.equals(zipEntry.getName())) {
				return zipInputStream;
			}
		}
		return null;
	}
}
