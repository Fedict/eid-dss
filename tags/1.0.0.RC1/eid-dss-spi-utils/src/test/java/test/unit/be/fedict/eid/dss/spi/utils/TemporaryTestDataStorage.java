package test.unit.be.fedict.eid.dss.spi.utils;

import be.fedict.eid.applet.service.signer.TemporaryDataStorage;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

class TemporaryTestDataStorage implements TemporaryDataStorage {

    private ByteArrayOutputStream outputStream;

    private Map<String, Serializable> attributes;

    public TemporaryTestDataStorage() {
        this.outputStream = new ByteArrayOutputStream();
        this.attributes = new HashMap<String, Serializable>();
    }

    public InputStream getTempInputStream() {
        byte[] data = this.outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        return inputStream;
    }

    public OutputStream getTempOutputStream() {
        return this.outputStream;
    }

    public Serializable getAttribute(String attributeName) {
        return this.attributes.get(attributeName);
    }

    public void setAttribute(String attributeName, Serializable attributeValue) {
        this.attributes.put(attributeName, attributeValue);
    }
}