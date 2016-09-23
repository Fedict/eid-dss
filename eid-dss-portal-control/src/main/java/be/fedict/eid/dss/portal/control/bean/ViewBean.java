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

package be.fedict.eid.dss.portal.control.bean;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.ejb.EJB;
import javax.ejb.Remove;
import javax.ejb.Stateful;

import org.apache.commons.io.IOUtils;
import org.jboss.ejb3.annotation.LocalBinding;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Out;
import org.jboss.seam.international.LocaleSelector;
import org.jboss.seam.log.Log;

import be.e_contract.dssp.client.DigitalSignatureServiceClient;
import be.e_contract.dssp.client.DigitalSignatureServiceSession;
import be.e_contract.dssp.client.SignatureInfo;
import be.e_contract.dssp.client.VerificationResult;
import be.e_contract.dssp.client.exception.ApplicationDocumentAuthorizedException;
import be.e_contract.dssp.client.exception.AuthenticationRequiredException;
import be.e_contract.dssp.client.exception.DocumentSignatureException;
import be.e_contract.dssp.client.exception.IncorrectSignatureTypeException;
import be.e_contract.dssp.client.exception.UnsupportedDocumentTypeException;
import be.e_contract.dssp.client.exception.UnsupportedSignatureTypeException;
import be.fedict.eid.dss.model.ConfigProperty;
import be.fedict.eid.dss.model.Configuration;
import be.fedict.eid.dss.portal.control.View;
import be.fedict.eid.dss.portal.control.state.SigningModel;
import be.fedict.eid.dss.portal.control.state.SigningModelRepository;

@Stateful
@Name("dssPortalView")
@LocalBinding(jndiBinding = "fedict/eid/dss/portal/ViewBean")
public class ViewBean implements View {

	@Logger
	private Log log;
	@EJB
	private Configuration configuration;
	@In
	private LocaleSelector localeSelector;
	@In(value = SigningModelRepository.ATTRIBUTE_SIGNING_MODEL, scope = ScopeType.SESSION, required = false)
	@Out(value = SigningModelRepository.ATTRIBUTE_SIGNING_MODEL, scope = ScopeType.SESSION, required = false)
	private SigningModel signingModel;

	@Remove
	@Destroy
	@Override
	public void destroy() {
	}

	@Override
	public void initialize() {
		if (signingModel.getState() == SigningModel.State.SIGN_COMPLETE) {
			fetchSignedDocument();
		}

		if (signingModel.getState() == SigningModel.State.UPLOADED || signingModel.getState() == SigningModel.State.SIGN_COMPLETE) {
			verifySignatures();
		}
	}

	private void fetchSignedDocument() {
		byte[] signedDocument = getDSSClient().downloadSignedDocument(signingModel.getDigitalSignatureServiceSession());
		signingModel.updateDocument(signedDocument);
	}

	private void verifySignatures() {
		List<SignatureInfo> signatureInfos = new ArrayList<>();
		try {
			VerificationResult verificationResult = getDSSClient().verify(signingModel.getContentType(), signingModel.getDocument());
			if (verificationResult != null) {
				signatureInfos = verificationResult.getSignatureInfos();
			}
		} catch (DocumentSignatureException e) {
			log.error("Cannot verify signatures: {0}", e.getMessage());
		} catch (UnsupportedDocumentTypeException e) {
			log.error("Document type not supported: {0}", e.getMessage());
		}

		if (signatureInfos.size() != 0) {
			signingModel.markSigned(signatureInfos);
		} else {
			signingModel.markUnsigned();
		}

		log.info("Number of signatures: {0}", signatureInfos.size());
		for (SignatureInfo signatureInfo : signatureInfos) {
			log.info("Signer: {0}", signatureInfo.getName());
			log.info("Signing time: {0}", signatureInfo.getSigningTime());
		}
	}

	@Override
	public void startSign() {
		try {
			trySign();
		} catch (UnsupportedDocumentTypeException e) {
			try {
				storeDocumentInZipFile();
				trySign();
			} catch (UnsupportedDocumentTypeException e2) {
				signingModel.markSignError("Unsupported document type.");
			}
		}
	}

	private void trySign() throws UnsupportedDocumentTypeException {
		DigitalSignatureServiceSession digitalSignatureServiceSession;
		try {
			digitalSignatureServiceSession = getDSSClient().uploadDocument(signingModel.getContentType(), signingModel.getDocument());
			log.debug("DSS Artifact ID: " + digitalSignatureServiceSession.getResponseId());

			signingModel.markSigning(digitalSignatureServiceSession);
		} catch (ApplicationDocumentAuthorizedException | AuthenticationRequiredException | IncorrectSignatureTypeException | UnsupportedSignatureTypeException e) {
			signingModel.markSignError(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	private void storeDocumentInZipFile() {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

			try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
				ZipEntry zipEntry = new ZipEntry(signingModel.getFileName());
				zipOutputStream.putNextEntry(zipEntry);
				IOUtils.write(signingModel.getDocument(), zipOutputStream);
			}

			signingModel.updateDocument(outputStream.toByteArray(), "application/zip", signingModel.getFileName() + ".zip");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getDssStartUrl() {
		return getConfigurationValue(ConfigProperty.DSS_WS_START);
	}

	@Override
	public SigningModel getSigningModel() {
		return signingModel;
	}

	private DigitalSignatureServiceClient getDSSClient() {
		DigitalSignatureServiceClient dssClient = new DigitalSignatureServiceClient(getConfigurationValue(ConfigProperty.DSS_WS_URL));
		dssClient.setCredentials(getConfigurationValue(ConfigProperty.DSS_WS_USERNAME), getConfigurationValue(ConfigProperty.DSS_WS_PASSWORD));
		return dssClient;
	}

	private String getConfigurationValue(ConfigProperty configProperty) {
		return this.configuration.getValue(configProperty, String.class);
	}
}
