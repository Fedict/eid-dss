package be.fedict.eid.dss.portal.control.state;

import static be.fedict.eid.dss.portal.control.util.SeamUtil.getComponent;
import static be.fedict.eid.dss.portal.control.util.SeamUtil.getRequest;
import static java.util.Collections.emptyList;

import java.io.Serializable;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.international.LocaleSelector;

import be.e_contract.dssp.client.DigitalSignatureServiceSession;
import be.e_contract.dssp.client.PendingRequestFactory;
import be.e_contract.dssp.client.SignatureInfo;

public class SigningModel implements Serializable {

	private State state;

	private String fileName;
	private String contentType;
	private byte[] document;

	private String signError;
	private List<SignatureInfo> signatureInfos;
	private DigitalSignatureServiceSession digitalSignatureServiceSession;

	public SigningModel(String fileName, String contentType, byte[] document) {
		setState(State.UPLOADED);

		this.fileName = fileName;
		this.contentType = contentType;
		this.document = document;
	}

	public State getState() {
		return state;
	}

	public String getFileName() {
		return fileName;
	}

	public String getContentType() {
		return contentType;
	}

	public byte[] getDocument() {
		return document;
	}

	public List<SignatureInfo> getSignatureInfos() {
		return signatureInfos;
	}

	public String getSignError() {
		return signError;
	}

	public DigitalSignatureServiceSession getDigitalSignatureServiceSession() {
		return digitalSignatureServiceSession;
	}

	public String getPendingRequest() {
		HttpServletRequest request = getRequest();
		String target = String.format("%1s://%2s:%3s%4s/dss-response", request.getScheme(), request.getServerName(), request.getServerPort(), request.getContextPath());
		String language = getComponent(LocaleSelector.class).getLanguage();

		return PendingRequestFactory.createPendingRequest(digitalSignatureServiceSession, target, language);
	}

	private void setState(State state) {
		this.state = state;
	}


	public void markSignError(String signError) {
		setState(State.SIGN_ERROR);

		this.signError = signError;
		this.digitalSignatureServiceSession = null;
		this.signatureInfos = emptyList();
	}

	public void markSignComplete() {
		setState(State.SIGN_COMPLETE);

		this.signatureInfos = emptyList();
		this.signError = null;
	}

	public void markSigned(List<SignatureInfo> signatureInfos) {
		setState(State.SIGNED);

		this.signatureInfos = signatureInfos;
		this.digitalSignatureServiceSession = null;
		this.signError = null;
	}

	public void markUnsigned() {
		setState(State.UNSIGNED);

		this.signatureInfos = emptyList();
		this.signError = null;
	}

	public void markSigning(DigitalSignatureServiceSession digitalSignatureServiceSession) {
		setState(State.SIGNING);

		this.digitalSignatureServiceSession = digitalSignatureServiceSession;
	}

	public void updateDocument(byte[] document) {
		this.document = document;
	}

	public enum State {
		UPLOADED,
		UNSIGNED,
		SIGNING,
		SIGNED,
		SIGN_COMPLETE,
		SIGN_ERROR
	}
}
