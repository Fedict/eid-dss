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

package be.fedict.eid.dss.sp.bean;

import be.fedict.eid.dss.sp.servlet.UploadServlet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.util.encoders.Base64;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

public class SPBean {

    private static final Log LOG = LogFactory.getLog(SPBean.class);

    private ServletRequest request;

    private String signatureRequest;
    private String contentType;

    private String relayState;
    private String language;

    private String destination;
    private String target;

    public ServletRequest getRequest() {
        return this.request;
    }

    public void setRequest(ServletRequest request) {

        this.request = request;
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;

        byte[] document = (byte[]) httpServletRequest.getSession().getAttribute(
                UploadServlet.DOCUMENT_SESSION_ATTRIBUTE);
        if (null != document) {

            this.signatureRequest = new String(Base64.encode(document));
            this.contentType = (String) httpServletRequest.getSession().getAttribute(
                    UploadServlet.CONTENT_TYPE_SESSION_ATTRIBUTE);

            this.relayState = UUID.randomUUID().toString();
            LOG.debug("RelayState: " + this.relayState);
            this.language = "en";

            this.destination = "../eid-dss/protocol/simple";
            this.target = httpServletRequest.getContextPath() + "/dss-response";

            // store data on session for response handling
            httpServletRequest.getSession().setAttribute("ContentType", this.contentType);
            httpServletRequest.getSession().setAttribute("target", this.target);
            httpServletRequest.getSession().setAttribute("RelayState", this.relayState);
            httpServletRequest.getSession().setAttribute("SignatureRequest", this.signatureRequest);
        }
    }

    public String getSignatureRequest() {
        return this.signatureRequest;
    }

    public void setSignatureRequest(String signatureRequest) {
        this.signatureRequest = signatureRequest;
    }

    public String getContentType() {
        return this.contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getRelayState() {
        return this.relayState;
    }

    public void setRelayState(String relayState) {
        this.relayState = relayState;
    }

    public String getDestination() {
        return this.destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getTarget() {
        return this.target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
