package be.fedict.eid.dss.admin.portal.control.bean;

import be.fedict.eid.dss.admin.portal.control.AdminConstants;
import be.fedict.eid.dss.admin.portal.control.XmlSchema;
import be.fedict.eid.dss.entity.XmlSchemaEntity;
import be.fedict.eid.dss.model.XmlSchemaManager;
import be.fedict.eid.dss.model.exception.ExistingXmlSchemaException;
import be.fedict.eid.dss.model.exception.InvalidXmlSchemaException;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.annotations.*;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.annotations.datamodel.DataModelSelection;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.log.Log;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;
import java.io.InputStream;
import java.util.List;

@Stateful
@Name("dssXmlSchema")
@LocalBinding(jndiBinding = AdminConstants.ADMIN_JNDI_CONTEXT + "XmlSchemaBean")
public class XmlSchemaBean implements XmlSchema {

    @Logger
    private Log log;

    private InputStream uploadedFile;

    private String revision;

    @DataModel
    private List<XmlSchemaEntity> dssXmlSchemaList;

    @DataModelSelection
    private XmlSchemaEntity selectedXmlSchema;

    @EJB
    private XmlSchemaManager xmlSchemaManager;

    @In
    private FacesMessages facesMessages;

    @Remove
    @Destroy
    @Override
    public void destroy() {
        this.log.debug("destroy");
    }

    @Override
    public void add() {
        this.log.debug("add #0", this.revision);
        if (null == this.uploadedFile) {
            this.facesMessages.addToControl("file", "missing XML schema");
            return;
        }
        try {
            this.xmlSchemaManager.add(this.revision, this.uploadedFile);
        } catch (InvalidXmlSchemaException e) {
            this.facesMessages.addToControl("file", "not a valid XML schema");
            return;
        } catch (ExistingXmlSchemaException e) {
            this.facesMessages.addToControl("file", "existing XML schema");
            return;
        }
        initList();
    }

    @Override
    public void delete() {
        this.log.debug("delete: " + this.selectedXmlSchema.getNamespace());
        this.xmlSchemaManager.delete(this.selectedXmlSchema.getNamespace());
        initList();
    }

    @Override
    public InputStream getUploadedFile() {
        return this.uploadedFile;
    }

    @Override
    public void setUploadedFile(InputStream uploadedFile) {
        this.uploadedFile = uploadedFile;
    }

    @Override
    @Factory("dssXmlSchemaList")
    public void initList() {
        this.dssXmlSchemaList = this.xmlSchemaManager.getXmlSchemas();
    }

    @Override
    public String getRevision() {
        return this.revision;
    }

    @Override
    public void setRevision(String revision) {
        this.revision = revision;
    }
}
