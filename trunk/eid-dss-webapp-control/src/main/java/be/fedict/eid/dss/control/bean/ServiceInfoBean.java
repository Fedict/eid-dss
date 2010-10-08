package be.fedict.eid.dss.control.bean;

import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.log.Log;

import be.fedict.eid.dss.control.ServiceInfo;
import be.fedict.eid.dss.model.IdentityService;
import be.fedict.eid.dss.model.ServicesManager;
import be.fedict.eid.dss.model.XmlSchemaManager;
import be.fedict.eid.dss.model.XmlStyleSheetManager;

@Stateful
@Name("dssServiceInfo")
@LocalBinding(jndiBinding = "fedict/eid/dss/ServiceInfoBean")
public class ServiceInfoBean implements ServiceInfo {

	@Logger
	private Log log;

	@SuppressWarnings("unused")
	@DataModel
	private List<String> dssDocumentFormatList;

	@SuppressWarnings("unused")
	@DataModel
	private List<String> dssXmlSchemaNamespaces;

	@SuppressWarnings("unused")
	@DataModel
	private List<String> dssXmlStyleSheetNamespaces;

	@EJB
	private ServicesManager servicesManager;

	@EJB
	private XmlSchemaManager xmlSchemaManager;

	@EJB
	private XmlStyleSheetManager xmlStyleSheetManager;

	@EJB
	private IdentityService identityService;

	@Remove
	@Destroy
	@Override
	public void destroy() {
		this.log.debug("destroy");
	}

	@Override
	@Factory("dssDocumentFormatList")
	public void initDocumentFormatList() {
		this.dssDocumentFormatList = this.servicesManager
				.getSupportedDocumentFormats();
	}

	@Override
	@Factory("dssXmlSchemaNamespaces")
	public void initXmlSchemaNamespacesList() {
		this.dssXmlSchemaNamespaces = this.xmlSchemaManager
				.getXmlSchemaNamespaces();
	}

	@Override
	@Factory("dssXmlStyleSheetNamespaces")
	public void initXmlStyleSheetNamespacesList() {
		this.dssXmlStyleSheetNamespaces = this.xmlStyleSheetManager
				.getXmlStyleSheetNamespaces();
	}

	@Override
	@Factory("dssServiceFingerprint")
	public String getServiceFingerprint() {
		return this.identityService.getIdentityFingerprint();
	}
}
