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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.w3c.dom.Node;

/**
 * Helper class to build inputs for time-stamps. The digests for time-stamps are
 * usually calculated over a concatenations of byte-streams, resulting from
 * nodes and/or processed {@code Reference}s, with the proper canonicalization
 * if needed. This class provides methods to build a sequential input by adding
 * DOM {@code Node}s.
 * 
 * @author Frank Cornelis
 */
public class TimeStampDigestInput {

	private static final Log LOG = LogFactory
			.getLog(TimeStampDigestInput.class);

	private final String canonMethodUri;
	private final List<Node> nodes;

	static {
		org.apache.xml.security.Init.init();
	}

	/**
	 * @param canonMethodUri
	 *            the canonicalization method to be used, if needed
	 * @throws IllegalArgumentException
	 *             if {@code canonMethodUri} is {@code null}
	 */
	public TimeStampDigestInput(String canonMethodUri) {

		LOG.debug("canonMethodUri: " + canonMethodUri);

		if (null == canonMethodUri) {
			throw new IllegalArgumentException("c14n algo URI is null");
		}

		this.canonMethodUri = canonMethodUri;
		this.nodes = new LinkedList<Node>();
	}

	/**
	 * Adds a {@code Node} to the input. The node is canonicalized.
	 * 
	 * @param n
	 *            the node to be added
	 * @throws IllegalArgumentException
	 *             if {@code n} is {@code null}
	 */
	public void addNode(Node n) {
		if (null == n) {
			throw new IllegalArgumentException("DOM node is null");
		}
		LOG.debug("adding digest node: " + n.getLocalName());
		this.nodes.add(n);
	}

	/**
	 * Gets the octet-stream corresponding to the actual state of the input.
	 * 
	 * @return the octet-stream (always a new instance)
	 */
	public byte[] getBytes() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			for (Node node : this.nodes) {
				/*
				 * We have to re-init the canonicalizer each time, else the
				 * namespaces will be cached and will eventually be missing from
				 * the canonicalized nodes.
				 */
				Canonicalizer c14n;
				try {
					c14n = Canonicalizer.getInstance(this.canonMethodUri);
				} catch (InvalidCanonicalizerException e) {
					throw new RuntimeException("c14n algo error: "
							+ e.getMessage(), e);
				}
				baos.write(c14n.canonicalizeSubtree(node));
			}
		} catch (CanonicalizationException e) {
			throw new RuntimeException("c14n error: " + e.getMessage(), e);
		} catch (IOException e) {
			throw new RuntimeException("I/O error: " + e.getMessage(), e);
		}
		return baos.toByteArray();
	}
}
