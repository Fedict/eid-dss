package be.fedict.eid.dss.portal.control.state;

import javax.servlet.http.HttpSession;

public class SigningModelRepository {

	public static final String ATTRIBUTE_SIGNING_MODEL = "be.fedict.dss.portal.signingModel";

	public static SigningModel get(HttpSession httpSession) {
		SigningModel signingModel = (SigningModel) httpSession.getAttribute(ATTRIBUTE_SIGNING_MODEL);
		if (signingModel == null) {
			throw new IllegalStateException("Cannot find signing model.");
		}

		return signingModel;
	}

	public static void set(HttpSession httpSession, SigningModel signingModel) {
		httpSession.setAttribute(ATTRIBUTE_SIGNING_MODEL, signingModel);
	}

}
