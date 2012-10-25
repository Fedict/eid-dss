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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.commons.io.IOUtils;
import org.w3c.dom.ls.LSInput;

public class SignatureServiceLSInput implements LSInput {

	private final byte[] data;

	private final String publicId;

	private final String systemId;

	private final String baseUri;

	public SignatureServiceLSInput(byte[] data, String publicId,
			String systemId, String baseUri) {
		this.data = data;
		this.publicId = publicId;
		this.systemId = systemId;
		this.baseUri = baseUri;
	}

	public Reader getCharacterStream() {
		InputStream inputStream = getByteStream();
		BufferedReader reader = new BufferedReader(new InputStreamReader(
				inputStream));
		return reader;
	}

	public void setCharacterStream(Reader characterStream) {
		throw new UnsupportedOperationException();
	}

	public InputStream getByteStream() {
		return new ByteArrayInputStream(this.data);
	}

	public void setByteStream(InputStream byteStream) {
		throw new UnsupportedOperationException();
	}

	public String getStringData() {
		InputStream inputStream = getByteStream();
		String stringData;
		try {
			stringData = IOUtils.toString(inputStream);
		} catch (IOException e) {
			throw new RuntimeException("I/O error: " + e.getMessage(), e);
		}
		return stringData;
	}

	public void setStringData(String stringData) {
		throw new UnsupportedOperationException();
	}

	public String getSystemId() {
		return this.systemId;
	}

	public void setSystemId(String systemId) {
		throw new UnsupportedOperationException();
	}

	public String getPublicId() {
		return this.publicId;
	}

	public void setPublicId(String publicId) {
		throw new UnsupportedOperationException();

	}

	public String getBaseURI() {
		return this.baseUri;
	}

	public void setBaseURI(String baseURI) {
		throw new UnsupportedOperationException();
	}

	public String getEncoding() {
		return "UTF-8";
	}

	public void setEncoding(String encoding) {
		throw new UnsupportedOperationException();
	}

	public boolean getCertifiedText() {
		throw new UnsupportedOperationException();
	}

	public void setCertifiedText(boolean certifiedText) {
		throw new UnsupportedOperationException();
	}
}
