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
        <asp:Button ID="Button1" runat="server" OnClick="UploadButton_Click" Text="Upload Document" />
        <asp:HiddenField runat="server" ID="SignatureRequest" />
        <asp:HiddenField runat="server" ID="ContentType" />
        <asp:HiddenField runat="server" ID="RelayState" />
        <asp:HiddenField runat="server" ID="target" />
        <asp:HiddenField runat="server" ID="Language" />
    </div>
    </form>
    <asp:Label ID="Label1" runat="server"></asp:Label>
</body>
</html>
