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

package be.fedict.eid.dss.spi.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.signature.XMLSignatureInput;
import org.apache.xml.security.transforms.Transform;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Helper class to build inputs for time-stamps. The digests for time-stamps are
 * usually calculated over a concatenations of byte-streams, resulting from nodes
 * and/or processed {@code Reference}s, with the proper canonicalization if needed.
 * This class provides methods to build a sequential input by adding DOM {@code Node}s
 * or {@code Reference}s.
 *
 * @author http://code.google.com/p/xades4j
 */
public class TimeStampDigestInput {

    private static final Log LOG = LogFactory.getLog(TimeStampDigestInput.class);

    private final String canonMethodUri;
    private final ByteArrayOutputStream digestInput;

    static {
        org.apache.xml.security.Init.init();
    }

    /**
     * @param canonMethodUri the canonicalization method to be used, if needed
     * @throws NullPointerException if {@code canonMethodUri} is {@code null}
     */
    public TimeStampDigestInput(String canonMethodUri) {

        LOG.debug("canonMethodUri: " + canonMethodUri);

        if (null == canonMethodUri) {
            throw new NullPointerException();
        }

        this.canonMethodUri = canonMethodUri;
        this.digestInput = new ByteArrayOutputStream();
    }

    /**
     * Adds a {@code Node} to the input. The node is canonicalized.
     *
     * @param n the node to be added
     * @throws NullPointerException if {@code n} is {@code null}
     */
    public void addNode(Node n) {
        if (null == n) {
            throw new NullPointerException();
        }

        addToDigestInput(new XMLSignatureInput(n), n.getOwnerDocument());
    }

    private void addToDigestInput(XMLSignatureInput refData, Document doc) {
        try {
            if (refData.isNodeSet() || refData.isElement()) {
                Transform t = Transform.getInstance(doc, canonMethodUri);
                refData = t.performTransform(refData);
                // Fall through to add the bytes resulting from the canonicalization.
            }

            if (refData.isByteArray()) {
                digestInput.write(refData.getBytes());
            } else if (refData.isOctetStream()) {
                readWrite(refData.getOctetStream(), digestInput);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets the octet-stream corresponding to the actual state of the input.
     *
     * @return the octet-stream (always a new instance)
     */
    public byte[] getBytes() {
        return digestInput.toByteArray();
    }

    public static void readWrite(InputStream is, OutputStream os) throws IOException {
        byte[] buf = new byte[512];
        int nRead;
        while ((nRead = is.read(buf)) != -1) {
            os.write(buf, 0, nRead);
        }
    }
}
