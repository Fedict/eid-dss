using System;
using System.Collections.Generic;
using System.Text;

using DSSWSNamespace;
using System.Security.Cryptography.X509Certificates;
using System.Net.Security;
using System.ServiceModel;
using System.Net;
using System.Xml;
using System.Xml.Serialization;
using System.IO;



namespace eid_dss_sdk_dotnet
{
    public class DigitalSignatureServiceClientImpl : DigitalSignatureServiceClient
    {
        private String location;

        private DigitalSignatureServicePortTypeClient client;

        private X509Certificate sslCertificate;

        public DigitalSignatureServiceClientImpl(String location)
        {
            this.location = location;
        }

        private void setupClient()
        {
            EndpointAddress remoteAddress = new EndpointAddress(this.location);

            bool sslLocation = this.location.StartsWith("https") ? true : false;
            if (sslLocation)
            {
                if (null != this.sslCertificate)
                {
                    /*
                     * Setup SSL validation
                     */
                    Console.WriteLine("SSL Validation active");
                    ServicePointManager.ServerCertificateValidationCallback =
                        new RemoteCertificateValidationCallback(CertificateValidationCallback);
                }
                else
                {
                    Console.WriteLine("Accept ANY SSL Certificate");
                    ServicePointManager.ServerCertificateValidationCallback =
                        new RemoteCertificateValidationCallback(WCFUtil.AnyCertificateValidationCallback);
                }
            }

            if (null == this.client)
            {
                // Setup basic client
                if (sslLocation)
                {
                    this.client = new DigitalSignatureServicePortTypeClient(
                        WCFUtil.BasicHttpOverSSLBinding(), remoteAddress);
                }
                else
                {
                    this.client = new DigitalSignatureServicePortTypeClient(
                        new BasicHttpBinding(), remoteAddress);
                }

                // Add logging behaviour
                this.client.Endpoint.Behaviors.Add(new LoggingBehavior());
            }
        }

        public void configureSsl(X509Certificate2 sslCertificate)
        {
            this.sslCertificate = sslCertificate;
        }

        private bool CertificateValidationCallback(Object sender, X509Certificate certificate,
            X509Chain chain, SslPolicyErrors sslPolicyErrors)
        {
            Console.WriteLine("Certificate Validation Callback");
            bool result = certificate.Equals(this.sslCertificate);
            Console.WriteLine("TLS Authn Result: " + result);
            return result;
        }

        public bool verify(byte[] signedDocument, string mimeType)
        {
            ResponseBaseType response = doVerification(signedDocument, mimeType, false, false);

            String resultminor = validateResult(response);
            if (null == resultminor)
            {
                throw new SystemException("Missing ResultMinor");
            }

            if (DSSConstants.RESULT_MINOR_VALID_SIGNATURE.Equals(resultminor) ||
                DSSConstants.RESULT_MINOR_VALID_MULTI_SIGNATURES.Equals(resultminor))
            {
                return true;
            }

            return false;
        }

        private ResponseBaseType doVerification(byte[] documentData, String mimeType,
            bool returnSignerIdentity, bool returnVerificationReport)
        {

            Console.WriteLine("Verify");
            // setup the client
            setupClient();

            String requestId = "dss-verify-request-" + Guid.NewGuid().ToString();
            VerifyRequest verifyRequest = new VerifyRequest();
            verifyRequest.RequestID = requestId;

            AnyType optionalInputs = new AnyType();
            if (returnSignerIdentity)
            {
                // TODO: ...
                // XmlElement el = new XmlElement();
                //                DSSXSDNamespace.Ret
            }

            if (returnVerificationReport)
            {
                DSSXSDNamespace.ReturnVerificationReport returnVerificationReportElement =
                    new DSSXSDNamespace.ReturnVerificationReport();
                returnVerificationReportElement.IncludeVerifier = false;
                returnVerificationReportElement.IncludeCertificateValues = true;
                returnVerificationReportElement.ReportDetailLevel =
                    "urn:oasis:names:tc:dss-x:1.0:profiles:verificationreport:reportdetail:noDetails";

                XmlElement e = toDom("ReturnVerificationReport", DSSConstants.VR_NAMESPACE,
                    returnVerificationReportElement, typeof(DSSXSDNamespace.ReturnVerificationReport));

                optionalInputs.Any = new XmlElement[] { e };
            }

            verifyRequest.InputDocuments = getInputDocuments(documentData, mimeType);

            // operate
            ResponseBaseType response = this.client.verify(verifyRequest);

            // check response
            checkResponse(response, verifyRequest.RequestID);

            return response;
        }

        private InputDocuments getInputDocuments(byte[] documentData, String mimeType)
        {
            InputDocuments inputDocuments = new InputDocuments();

            DocumentType document = new DocumentType();
            if (null == mimeType || mimeType.Equals("text/xml"))
            {
                document.Item = documentData;
            }
            else
            {
                Base64Data base64Data = new Base64Data();
                base64Data.MimeType = mimeType;
                base64Data.Value = documentData;
                document.Item = base64Data;
            }

            inputDocuments.Items = new object[] { document };
            return inputDocuments;
        }

        private void checkResponse(ResponseBaseType response, String requestId)
        {
            if (null == response)
            {
                throw new SystemException("No response returned");
            }
            String responseRequestId = response.RequestID;
            if (null == responseRequestId)
            {
                throw new SystemException("Missing Response.RequestID");
            }
            if (!responseRequestId.Equals(requestId))
            {
                throw new SystemException("Incorrect Response.RequestID");
            }
        }

        private String validateResult(ResponseBaseType response) 
        {
            Result result = response.Result;
            String resultMajor = result.ResultMajor;
            String resultMinor = result.ResultMinor;
            Console.WriteLine("result major: " + resultMajor);
            if (!DSSConstants.RESULT_MAJOR_SUCCESS.Equals(resultMajor))
            {
                Console.WriteLine("result minor: " + resultMinor);
                if (null != resultMinor && resultMinor.Equals(
                    DSSConstants.RESULT_MINOR_NOT_PARSEABLE_XML_DOCUMENT))
                {
                    throw new NotParseableXMLDocumentException();
                }
                throw new SystemException("unsuccessful result: " + resultMajor);
            }
            return resultMinor;
        }


        private XmlElement toDom(String elementName, String ns, Object o, Type type)
        {
            // serialize to DOM
            XmlSerializerNamespaces namespaces = new XmlSerializerNamespaces();
            namespaces.Add("dss", DSSConstants.DSS_NAMESPACE);
            namespaces.Add("vr", DSSConstants.VR_NAMESPACE);
            namespaces.Add("artifact", DSSConstants.ARTIFACT_NAMESPACE);

            XmlRootAttribute xRoot = new XmlRootAttribute();
            xRoot.ElementName = elementName;
            xRoot.Namespace = ns;
            XmlSerializer serializer = new XmlSerializer(type, xRoot);
            MemoryStream memoryStream = new MemoryStream();
            XmlTextWriter xmlTextWriter = new XmlTextWriter(memoryStream, Encoding.UTF8);
            serializer.Serialize(xmlTextWriter, o, namespaces);

            XmlDocument xmlDocument = new XmlDocument();
            memoryStream.Seek(0, SeekOrigin.Begin);
            xmlDocument.Load(memoryStream);

            return (XmlElement)xmlDocument.ChildNodes.Item(0);
        }
    }
}
