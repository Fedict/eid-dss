using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.UI;
using System.Web.UI.WebControls;
using eid_dss_sdk_dotnet;

namespace DSSTestSP
{
    public partial class _Default : System.Web.UI.Page
    {
        public const String SIGNATURE_REQUEST_SESSION_PARAM = "SignatureRequest";
        public const String RELAY_STATE_SESSION_PARAM = "RelayState";
        public const String TARGET_SESSION_PARAM = "target";

        protected void Page_Load(object sender, EventArgs e)        
        {
            // inspect Session for SignatureRequest Data
            String signatureRequest = (String)Session[SIGNATURE_REQUEST_SESSION_PARAM];
            String relayState = (String)Session[RELAY_STATE_SESSION_PARAM];
            String target = (String)Session[TARGET_SESSION_PARAM];

            if (null != signatureRequest)
            {
                SignatureResponseProcessor processor = new SignatureResponseProcessor(null);
                try
                {
                    SignatureReponse signatureResponse = processor.process(Page.Request, target, signatureRequest, null, relayState);
                    // show results
                    if (null != signatureResponse)
                    {
                        this.Label1.Text = "Valid DSS Response.<br>" +
                            "SignatureCertificate.Subject: " + signatureResponse.getSignatureCertificate().Subject;
                        this.FileUpload1.Visible = false;
                        this.Button1.Visible = false;
                    }
                }
                catch (SignatureResponseProcessorException ex)
                {
                    this.Label1.Text = "Invalid DSS Response: " + ex.Message;
                }
            }
        }

        protected void UploadButton_Click(object sender, EventArgs e)
        {
            if (FileUpload1.HasFile)
                try
                {
                    byte[] doc = new byte[FileUpload1.PostedFile.ContentLength];
                    FileUpload1.PostedFile.InputStream.Read(doc, 0, FileUpload1.PostedFile.ContentLength);

                    SignatureRequest.Value = Convert.ToBase64String(doc);
                    ContentType.Value = FileUpload1.PostedFile.ContentType;
                    RelayState.Value = "foo";
                    target.Value = Request.Url.ToString();
                    Language.Value = "en";

                    // store request state on session
                    Session[SIGNATURE_REQUEST_SESSION_PARAM] = SignatureRequest.Value;
                    Session[RELAY_STATE_SESSION_PARAM] = RelayState.Value;
                    Session[TARGET_SESSION_PARAM] = target.Value;

                    SignForm.Action = "https://sebeco-dev-11:8443/eid-dss/protocol/simple";
                    Button1.Text = "Sign Document";

                    // hide fileupload
                    FileUpload1.Visible = false;
                    FileUpload1.Enabled = false;

                    // display some info
                    Label1.Text = "File name: " +
                         FileUpload1.PostedFile.FileName + "<br>" +
                         FileUpload1.PostedFile.ContentLength + " kb<br>" +
                         "Content type: " +
                         FileUpload1.PostedFile.ContentType;
                }
                catch (Exception ex)
                {
                    Label1.Text = "ERROR: " + ex.Message.ToString();
                }
            else
            {
                Label1.Text = "You have not specified a file.";
            }
        }
    }
}
