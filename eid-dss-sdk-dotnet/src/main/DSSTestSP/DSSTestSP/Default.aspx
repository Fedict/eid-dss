<%@ Page Language="C#" AutoEventWireup="true" CodeBehind="Default.aspx.cs" Inherits="DSSTestSP._Default" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head runat="server">
    <title></title>
</head>
<body>
    <h1>Test SP</h1>
    <form id="SignForm" runat="server">
    <div>
        <asp:FileUpload ID="FileUpload1" runat="server" />
        <!-- Signature Request POST parameters -->
        <asp:HiddenField runat="server" ID="SignatureRequest" />
        <asp:HiddenField runat="server" ID="SignatureRequestId" />
        <asp:HiddenField runat="server" ID="ContentType" />
        <asp:HiddenField runat="server" ID="RelayState" />
        <asp:HiddenField runat="server" ID="target" />
        <asp:HiddenField runat="server" ID="language" />
        <!-- Signature Request Service Signature POST parameters -->
        <asp:HiddenField runat="server" ID="ServiceSigned" />
        <asp:HiddenField runat="server" ID="ServiceSignature" />
        <asp:HiddenField runat="server" ID="ServiceCertificateChainSize" />
        <asp:HiddenField runat="server" ID="ServiceCertificate"   />
        <!-- Controls -->
        <p />
        <asp:Button ID="Button1" runat="server" OnClick="Button1_Click" />
        <asp:Button ID="Button2" runat="server" OnClick="Button2_Click" />
        <asp:Button ID="Button3" runat="server" OnClick="Button3_Click" />
        <asp:Button ID="Button4" runat="server" OnClick="Button4_Click" />
    </div>
    </form>
    <asp:Label ID="Label1" runat="server"></asp:Label>
</body>
</html>
