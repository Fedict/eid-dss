package be.fedict.eid.dss.portal.control.util;

import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;

import org.jboss.seam.Component;

public class SeamUtil {

	public static <T> T getComponent(Class<T> componentClass) {
		return componentClass.cast(Component.getInstance(componentClass));
	}

	public static HttpServletRequest getRequest() {
		return (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
	}

}
