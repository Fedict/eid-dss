<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
<title>SP Artifact (Signed) DSS Request</title>
</head>
<body>

	Uploading document for signing...

	<jsp:useBean id="sp" scope="request"
		class="be.fedict.eid.dss.sp.bean.SPBean" />
	<jsp:setProperty name="sp" property="artifactRequestSigned"
		value="<%= request %>" />

	<form id="dss-request-form" method="post" action="${sp.destination}">

		<input type="hidden" name="SignatureRequestId"
			value="${sp.signatureRequestId}" /> <input type="hidden"
			name="RelayState" value="${sp.relayState}" /> <input type="hidden"
			name="target" value="${sp.target}" /> <input type="hidden"
			name="language" value="${sp.language}" />

		<!-- Service Signature -->
		<input type="hidden" name="ServiceSigned" value="${sp.serviceSigned}" />
		<input type="hidden" name="ServiceSignature"
			value="${sp.serviceSignature}" /> <input type="hidden"
			name="ServiceCertificateChainSize"
			value="${sp.serviceCertificateChainSize}" /> <input type="hidden"
			name="ServiceCertificate.1" value="${sp.serviceCertificate}" /> <input
			type="submit" value="Submit" />
	</form>

	<script type="text/javascript">
        document.getElementById('dss-request-form').submit();
    </script>
</body>
</html>