package be.fedict.eid.dss.portal.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.crypto.MarshalException;
import javax.xml.crypto.dsig.XMLSignatureException;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.xml.security.exceptions.Base64DecodingException;
import org.xml.sax.SAXException;

import be.e_contract.dssp.client.DigitalSignatureServiceSession;
import be.e_contract.dssp.client.SignResponseVerifier;
import be.e_contract.dssp.client.exception.ClientRuntimeException;
import be.e_contract.dssp.client.exception.SubjectNotAuthorizedException;
import be.e_contract.dssp.client.exception.UserCancelException;
import be.fedict.eid.dss.portal.control.state.SigningModel;
import be.fedict.eid.dss.portal.control.state.SigningModelRepository;

public class SignatureResponseServlet extends HttpServlet {

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		SigningModel signingModel = SigningModelRepository.get(request.getSession());

		String signResponse = request.getParameter("SignResponse");
		DigitalSignatureServiceSession digitalSignatureServiceSession = signingModel.getDigitalSignatureServiceSession();

		try {
			SignResponseVerifier.checkSignResponse(signResponse, digitalSignatureServiceSession);
			signingModel.markSignComplete();
		} catch (JAXBException | SAXException | MarshalException | Base64DecodingException | ParserConfigurationException | XMLSignatureException e) {
			signingModel.markSignError("Error parsing response document: " + e.getMessage());
		} catch (UserCancelException e) {
			signingModel.markSignError("Signing cancelled by user");
		} catch (SubjectNotAuthorizedException e) {
			signingModel.markSignError("User not authorized for signing");
		} catch (ClientRuntimeException e) {
			signingModel.markSignError("Client runtime error");
		}

		response.sendRedirect("view.seam");
	}

}
