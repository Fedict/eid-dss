<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN"
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<head>
<title>eID DSS - Test Service Provider (SP)</title>
</head>
<body>
	<jsp:useBean id="responseBean" scope="request"
		class="be.fedict.eid.dss.sp.bean.ResponseBean" />
	<jsp:setProperty name="responseBean" property="request"
		value="<%= request %>" />

	<h1>eID DSS - Test Service Provider (SP)</h1>

	<c:if test="${responseBean.signatureInfos != null}">

		<h2>Successfully signed the document.</h2>

        Download the document
        <a href="./download">here...</a>

		<p />

		<table border="1">
			<tr>
				<c:forEach var="signatureInfo"
					items="${responseBean.signatureInfos}">
					<tr>
						<th>Signing Time</th>
						<td>${signatureInfo.signingTime}</td>
					</tr>
					<tr>
						<th>Signer</th>
						<td>${signatureInfo.signer.subjectX500Principal}</td>
					</tr>
					<tr>
						<th>Role</th>
						<td>${signatureInfo.role}</td>
					</tr>
				</c:forEach>
			</tr>
		</table>
	</c:if>

	<p />

	<a href="./index.jsp">Sign something else...</a>

</body>
</html>