using System;
using System.Collections.Generic;
using System.Linq;
using System.Web;
using System.Web.UI;
using System.Web.UI.WebControls;
using eid_dss_sdk_dotnet;
using System.Runtime.Remoting.Metadata.W3cXsd2001;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;

namespace DSSTestSP
{
    public partial class _Default : System.Web.UI.Page
    {
        // Test SP configuration
        private static string TEST_DIRECTORY_PATH = "C:\\Users\\devel\\Documents\\Test\\";
        private static string CERT_DIRECTORY_PATH = TEST_DIRECTORY_PATH + "certificates\\";
        private static string TEST_PFX_PATH = CERT_DIRECTORY_PATH + "test.pfx";
        private static string TEST_PFX_PASSWORD = "secret";
        private static string TEST_CRT_PATH = CERT_DIRECTORY_PATH + "test.crt";

        // DSS Config parameters
        private const String dssLocation = "https://sebeco-dev-11:8443/eid-dss/protocol/simple";
        private const String dssWSLocation = "https://sebeco-dev-11:8443/eid-dss-ws/dss";
        private long maxReceivedMessageSize = 65536 * 4;
        private const String serviceFingerprint = "96964dfed390fc3a884d897f00bc4446cb9d9429";

        // session parameters
        public const String SIGNATURE_REQUEST_SESSION_PARAM = "SignatureRequest";
        public const String SIGNATURE_REQUEST_ID_SESSION_PARAM = "SignatureRequestId";
        public const String RELAY_STATE_SESSION_PARAM = "RelayState";
        public const String TARGET_SESSION_PARAM = "target";
        public const String CONTENT_TYPE_SESSION_PARAM = "ContentType";
        public const String SIGNED_DOCUMENT_SESSION_PARAM = "SignedDocument";

        protected void Page_Load(object sender, EventArgs e)
        {
            // inspect Session for SignatureRequest Data
            String signatureRequest = (String)Session[SIGNATURE_REQUEST_SESSION_PARAM];
            String signatureRequestId = (String)Session[SIGNATURE_REQUEST_ID_SESSION_PARAM];
            String relayState = (String)Session[RELAY_STATE_SESSION_PARAM];
            String target = (String)Session[TARGET_SESSION_PARAM];

            if (null != signatureRequest || null != signatureRequestId)
            {
                SignatureResponseProcessor processor = new SignatureResponseProcessor(
                    SoapHexBinary.Parse(serviceFingerprint).Value);
                try
                {
                    SignatureReponse signatureResponse = processor.Process(Page.Request, target,
                        signatureRequest, signatureRequestId, relayState);
                    if (null != signatureResponse)
                    {

                        byte[] signedDocument;
                        if (null != signatureRequestId)
                        {
                            // fetch signed document via WS
                            signedDocument = getClient().Retrieve(signatureResponse.SignatureResponseId);
                        }
                        else
                        {
                            signedDocument = signatureResponse.DecodedSignatureResponse;
                        }

                        // show results
                        this.Label1.Text = "Valid DSS Response.<br>" +
                            "SignatureCertificate.Subject: " + signatureResponse.SignatureCertificate.Subject;
                        this.Button1.Text = "Validate (WS)";
                        Session[SIGNED_DOCUMENT_SESSION_PARAM] = signedDocument;
                        hideRequest();
                    }
                }
                catch (SignatureResponseProcessorException ex)
                {
                    this.Label1.Text = "Invalid DSS Response: " + ex.Message;
                    hideRequest();
                }
            }
            else
            {
                // initialize gui
                Button1.Visible = true;
                Button2.Visible = true;
                Button3.Visible = true;
                Button4.Visible = true;
                Button1.Text = "Upload Document";
                Button2.Text = "Upload Document (Artifact)";
                Button3.Text = "Upload Document (Signed Request)";
                Button4.Text = "Upload Document (Signed Request) (Artifact)";
            }
        }

        // hides/disables the upload field, service fingerprint input, ... all what is needed to create a signature request.
        private void hideRequest()
        {
            FileUpload1.Visible = false;
            FileUpload1.Enabled = false;
            Button2.Visible = false;
            Button3.Visible = false;
            Button4.Visible = false;
        }

        private DigitalSignatureServiceClient getClient()
        {
            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(dssWSLocation);
            client.SetMaxReceivedMessageSize(maxReceivedMessageSize);
            client.SetLogging(true);
            return client;
        }


        private void SetSignatureRequest(bool signed, bool artifact, String languageValue)
        {
            if (FileUpload1.HasFile)
                try
                {
                    // read to be signed document
                    byte[] doc = new byte[FileUpload1.PostedFile.ContentLength];
                    FileUpload1.PostedFile.InputStream.Read(doc, 0, FileUpload1.PostedFile.ContentLength);

                    // construct post parameter values
                    String signatureRequestValue = null;
                    String signatureRequestIdValue = null;
                    String contentTypeValue = FileUpload1.PostedFile.ContentType;
                    String relayStateValue = Guid.NewGuid().ToString();
                    String targetValue = Request.Url.ToString();

                    if (artifact)
                    {
                        // upload using WS
                        StorageInfoDO storageInfo = getClient().Store(doc, FileUpload1.PostedFile.ContentType);
                        signatureRequestIdValue = storageInfo.Artifact;
                    }
                    else
                    {
                        signatureRequestValue = Convert.ToBase64String(doc);
                    }

                    // construct service signature if requested
                    ServiceSignatureDO serviceSignature = null;
                    if (signed)
                    {
                        RSACryptoServiceProvider rsa = KeyStoreUtil.GetPrivateKeyFromPfx(TEST_PFX_PATH, TEST_PFX_PASSWORD, true);
                        X509Certificate2 certificate = KeyStoreUtil.GetCertificateFromPfx(TEST_PFX_PATH, TEST_PFX_PASSWORD, true);
                        List<X509Certificate2> certificateChain = new List<X509Certificate2>();
                        certificateChain.Add(certificate);

                        serviceSignature = SignatureRequestUtil.CreateServiceSignature(rsa, certificateChain, signatureRequestValue,
                            signatureRequestIdValue, targetValue, languageValue, contentTypeValue, relayStateValue);
                    }

                    // set signature request post parameters
                    if (null != signatureRequestValue)
                    {
                        SignatureRequest.Value = signatureRequestValue;
                        SignatureRequestId.Visible = false;
                    }
                    else
                    {
                        SignatureRequest.Visible = false;
                        SignatureRequestId.Value = signatureRequestIdValue;
                    }
                    if (null != serviceSignature)
                    {
                        ServiceSigned.Value = serviceSignature.ServiceSigned;
                        ServiceSignature.Value = serviceSignature.ServiceSignature;
                        ServiceCertificateChainSize.Value = serviceSignature.ServiceCertificateChainSize;
                        ServiceCertificate.Value = serviceSignature.ServiceCertificates[0];
                        ServiceCertificate.ID = "ServiceCertificate.1";
                    }
                    else
                    {
                        ServiceSigned.Visible = false;
                        ServiceSignature.Visible = false;
                        ServiceCertificateChainSize.Visible = false;
                        ServiceCertificate.Visible = false;
                    }
                    ContentType.Value = contentTypeValue;
                    RelayState.Value = relayStateValue;
                    target.Value = targetValue;
                    language.Value = languageValue;

                    // store signature request state on session for response validation
                    Session[SIGNATURE_REQUEST_SESSION_PARAM] = signatureRequestValue;
                    Session[SIGNATURE_REQUEST_ID_SESSION_PARAM] = signatureRequestIdValue;
                    Session[RELAY_STATE_SESSION_PARAM] = relayStateValue;
                    Session[TARGET_SESSION_PARAM] = targetValue;
                    Session[CONTENT_TYPE_SESSION_PARAM] = contentTypeValue;

                    // ready for sign request
                    SignForm.Action = dssLocation;
                    Button1.Text = "Sign Document";

                    hideRequest();

                    // display some info
                    Label1.Text = "File name: " + FileUpload1.PostedFile.FileName + "<br>" +
                         FileUpload1.PostedFile.ContentLength + " kb<br>" +
                         "Content type: " + FileUpload1.PostedFile.ContentType + "<br>";

                    if (null != signatureRequestIdValue) Label1.Text += "Document ID: " + signatureRequestIdValue + "<br>";
                    if (null != serviceSignature) Label1.Text += "Service Signed: " + serviceSignature.ServiceSigned + "<br>";
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

        protected void Button1_Click(object sender, EventArgs e)
        {
            byte[] signedDocument = (byte[])Session[SIGNED_DOCUMENT_SESSION_PARAM];
            String contentType = (string) Session[CONTENT_TYPE_SESSION_PARAM];
            if (null != signedDocument)
            {
                // validate signed document over WS
                List<SignatureInfo> signatureInfos = getClient().VerifyWithSigners(signedDocument, contentType);
                Label1.Text = "SignatureInfos:<br>";
                foreach (SignatureInfo signatureInfo in signatureInfos)
                {
                    Label1.Text += "  * Signer: " + signatureInfo.Signer.Subject.ToString() + "<br>";
                    Label1.Text += "  * Time  : " + signatureInfo.SigningTime + "<br>";
                    Label1.Text += "  * Role  : " + signatureInfo.Role + "<br><br>";
                }
                Button1.Visible = false;

                // clear session, we are done
                Session.Abandon();
            }
            else
            {
                SetSignatureRequest(false, false, "en");
            }
        }

        protected void Button2_Click(object sender, EventArgs e)
        {
            SetSignatureRequest(false, true, "fr");
        }

        protected void Button3_Click(object sender, EventArgs e)
        {
            SetSignatureRequest(true, false, "nl");
        }

        protected void Button4_Click(object sender, EventArgs e)
        {
            SetSignatureRequest(true, true, "en");
        }
    }
}
