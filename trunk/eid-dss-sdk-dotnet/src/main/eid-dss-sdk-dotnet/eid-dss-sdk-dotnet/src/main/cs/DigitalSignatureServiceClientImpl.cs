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
using System.ServiceModel.Channels;



namespace eid_dss_sdk_dotnet
{
    public class DigitalSignatureServiceClientImpl : DigitalSignatureServiceClient
    {
        private String location;

        private bool logging = true;

        private long maxReceivedMessageSize = 0;

        private DigitalSignatureServicePortTypeClient client;

        private X509Certificate sslCertificate;

        public DigitalSignatureServiceClientImpl(String location)
        {
            this.location = location;
        }

        public void SetLogging(bool logging)
        {
            this.logging = logging;
        }

        public void SetMaxReceivedMessageSize(long maxReceivedMessageSize)
        {
            this.maxReceivedMessageSize = maxReceivedMessageSize;
        }

        private void SetupClient()
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
                Binding binding;
                if (sslLocation)
                {
                    binding = WCFUtil.BasicHttpOverSSLBinding(this.maxReceivedMessageSize);
                }
                else
                {
                    binding = new BasicHttpBinding();
                    if (this.maxReceivedMessageSize > 0) ((BasicHttpBinding)binding).MaxReceivedMessageSize = maxReceivedMessageSize;
                }
                this.client = new DigitalSignatureServicePortTypeClient(binding, remoteAddress);

                // Add logging behaviour
                if (this.logging)
                {
                    this.client.Endpoint.Behaviors.Add(new LoggingBehavior());
                }
            }
        }

        public void ConfigureSsl(X509Certificate2 sslCertificate)
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

        public bool Verify(byte[] signedDocument, string mimeType)
        {
            ResponseBaseType response = DoVerification(signedDocument, mimeType, false, false);

            String resultminor = ValidateResult(response);
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

        public List<SignatureInfo> VerifyWithSigners(byte[] signedDocument, String mimeType)
        {
            ResponseBaseType response = DoVerification(signedDocument, mimeType, false, true);

            ValidateResult(response);

            // TODO: parse verificationReport
            List<SignatureInfo> signers = new List<SignatureInfo>();
            DSSXSDNamespace.VerificationReportType verificationReport = FindVerificationReport(response);
            if (null == verificationReport)
            {
                return signers;
            }

            foreach (DSSXSDNamespace.IndividualReportType individualReport in verificationReport.IndividualReport)
            {

                if (!DSSConstants.RESULT_MAJOR_SUCCESS.Equals(individualReport.Result.ResultMajor))
                {
                    Console.WriteLine("WARNING: invalid VR result reported: " +
                        individualReport.Result.ResultMajor);
                    continue;
                }

                DSSXSDNamespace.SignedObjectIdentifierType signedObjectIdentifier
                    = individualReport.SignedObjectIdentifier;

                DateTime signingTime = signedObjectIdentifier.SignedProperties
                    .SignedSignatureProperties.SigningTime;
                X509Certificate signer = null;
                String role = null;

                foreach (XmlElement detail in individualReport.Details.Any)
                {
                    if (detail.NamespaceURI.Equals(DSSConstants.VR_NAMESPACE) &&
                        detail.LocalName.Equals("DetailedSignatureReport"))
                    {
                        DSSXSDNamespace.DetailedSignatureReportType detailedSignatureReport =
                            (DSSXSDNamespace.DetailedSignatureReportType)FromDom("DetailedSignatureReport",
                            DSSConstants.VR_NAMESPACE, detail,
                            typeof(DSSXSDNamespace.DetailedSignatureReportType));

                        DSSXSDNamespace.CertificateValidityType certificateValidity =
                            detailedSignatureReport.CertificatePathValidity
                                .PathValidityDetail.CertificateValidity[0];

                        byte[] encodedSigner = certificateValidity.CertificateValue;
                        signer = new X509Certificate(encodedSigner);

                        if (null != detailedSignatureReport.Properties)
                        {
                            DSSXSDNamespace.SignerRoleType1 signerRole = detailedSignatureReport.Properties
                                .SignedProperties.SignedSignatureProperties.SignerRole;
                            if (null != signerRole)
                            {
                                role = signerRole.ClaimedRoles[0].Any[0].Value;
                            }
                        }
                    }
                }

                if (null == signer)
                {
                    throw new SystemException("No signer certificate present in verification report.");
                }

                signers.Add(new SignatureInfo(signer, signingTime, role));
            }
            return signers;
        }

        private DSSXSDNamespace.VerificationReportType FindVerificationReport(ResponseBaseType responseBase)
        {
            if (null == responseBase.OptionalOutputs)
            {
                return null;
            }
            foreach (XmlElement optionalOutput in responseBase.OptionalOutputs.Any)
            {
                if (optionalOutput.NamespaceURI.Equals(DSSConstants.VR_NAMESPACE) &&
                    optionalOutput.LocalName.Equals("VerificationReport"))
                {
                    DSSXSDNamespace.VerificationReportType verificationReport =
                        (DSSXSDNamespace.VerificationReportType)FromDom("VerificationReport",
                        DSSConstants.VR_NAMESPACE, optionalOutput,
                        typeof(DSSXSDNamespace.VerificationReportType));
                    return verificationReport;
                }
            }

            return null;
        }

        private ResponseBaseType DoVerification(byte[] documentData, String mimeType,
            bool returnSignerIdentity, bool returnVerificationReport)
        {
            Console.WriteLine("Verify");

            // setup the client
            SetupClient();

            String requestId = "dss-verify-request-" + Guid.NewGuid().ToString();
            VerifyRequest verifyRequest = new VerifyRequest();
            verifyRequest.RequestID = requestId;

            AnyType optionalInputs = new AnyType();
            List<XmlElement> optionalInputElements = new List<XmlElement>();
            if (returnSignerIdentity)
            {
                XmlElement e = GetElement("dss", "ReturnSignerIdentity", DSSConstants.DSS_NAMESPACE);
                optionalInputElements.Add(e);
            }

            if (returnVerificationReport)
            {
                DSSXSDNamespace.ReturnVerificationReport returnVerificationReportElement =
                    new DSSXSDNamespace.ReturnVerificationReport();
                returnVerificationReportElement.IncludeVerifier = false;
                returnVerificationReportElement.IncludeCertificateValues = true;
                returnVerificationReportElement.ReportDetailLevel =
                    "urn:oasis:names:tc:dss-x:1.0:profiles:verificationreport:reportdetail:noDetails";

                XmlElement e = ToDom("ReturnVerificationReport", DSSConstants.VR_NAMESPACE,
                    returnVerificationReportElement, typeof(DSSXSDNamespace.ReturnVerificationReport));

                optionalInputElements.Add(e);
            }

            if (optionalInputElements.Count > 0)
            {
                optionalInputs.Any = optionalInputElements.ToArray();
                verifyRequest.OptionalInputs = optionalInputs;
            }

            verifyRequest.InputDocuments = GetInputDocuments(documentData, mimeType);

            // operate
            ResponseBaseType response = this.client.verify(verifyRequest);

            // check response
            CheckResponse(response, verifyRequest.RequestID);

            return response;
        }

        public StorageInfoDO Store(byte[] documentData, String contentType)
        {
            Console.WriteLine("Store");

            // setup the client
            SetupClient();

            // create SignRequest
            String requestId = "dss-sign-request-" + Guid.NewGuid().ToString();
            SignRequest signRequest = new SignRequest();
            signRequest.RequestID = requestId;
            signRequest.Profile = DSSConstants.ARTIFACT_NAMESPACE;

            // add "ReturnStorageInfo" optional input
            AnyType optionalInputs = new AnyType();
            XmlElement returnStorageInfoElement = GetElement("artifact", "ReturnStorageInfo", DSSConstants.ARTIFACT_NAMESPACE);
            optionalInputs.Any = new XmlElement[] { returnStorageInfoElement };
            signRequest.OptionalInputs = optionalInputs;

            // add document
            signRequest.InputDocuments = GetInputDocuments(documentData, contentType);

            // operate
            SignResponse signResponse = client.sign(signRequest);

            // parse response
            CheckResponse(signResponse, requestId);

            try
            {
                ValidateResult(signResponse);
            }
            catch (NotParseableXMLDocumentException e)
            {
                throw new SystemException(e.Message, e);
            }

            // check profile
            if (!signResponse.Profile.Equals(DSSConstants.ARTIFACT_NAMESPACE))
            {
                throw new SystemException("Unexpected SignResponse.Profile: " + signResponse.Profile);
            }

            // parse StorageInfo
            DSSXSDNamespace.StorageInfo storageInfo = FindStorageInfo(signResponse);
            if (null == storageInfo)
            {
                throw new SystemException("Missing StorageInfo");
            }

            return new StorageInfoDO(storageInfo.Identifier, storageInfo.Validity.NotBefore, storageInfo.Validity.NotAfter);
        }

        public byte[] Retrieve(String documentId)
        {
            Console.WriteLine("Retrieve");

            // setup client
            SetupClient();

            // create request
            String requestId = "dss-sign-request-" + Guid.NewGuid().ToString();
            SignRequest signRequest = new SignRequest();
            signRequest.RequestID = requestId;
            signRequest.Profile = DSSConstants.ARTIFACT_NAMESPACE;

            // add "ReturnStoredDocument" optional input
            AnyType optionalInputs = new AnyType();

            DSSXSDNamespace.ReturnStoredDocument returnStoredDocument = new DSSXSDNamespace.ReturnStoredDocument();
            returnStoredDocument.Identifier = documentId;

            XmlElement returnStoredDocumentElement = ToDom("ReturnStoredDocument", DSSConstants.ARTIFACT_NAMESPACE,
                returnStoredDocument, typeof(DSSXSDNamespace.ReturnStoredDocument));
            optionalInputs.Any = new XmlElement[] { returnStoredDocumentElement };
            signRequest.OptionalInputs = optionalInputs;

            // operate
            SignResponse signResponse = this.client.sign(signRequest);

            // parse response
            CheckResponse(signResponse, requestId);

            try
            {
                ValidateResult(signResponse);
            }
            catch (NotParseableXMLDocumentException e)
            {
                throw new SystemException(e.Message, e);
            }

            // check profile
            if (!signResponse.Profile.Equals(DSSConstants.ARTIFACT_NAMESPACE))
            {
                throw new SystemException("Unexpected SignResponse.Profile: " + signResponse.Profile);
            }

            // get document
            DSSXSDNamespace.DocumentWithSignature documentWithSignature = FindDocumentWithSignature(signResponse);
            if (null == documentWithSignature || null == documentWithSignature.Document || null == documentWithSignature.Document.Item)
            {
                throw new DocumentNotFoundException();
            }
            byte[] documentData;

            if (documentWithSignature.Document.Item is DSSXSDNamespace.Base64Data)
            {
                documentData = ((DSSXSDNamespace.Base64Data)documentWithSignature.Document.Item).Value;
            }
            else
            {
                documentData = (byte[])documentWithSignature.Document.Item;
            }
            return documentData;
        }

        private DSSXSDNamespace.DocumentWithSignature FindDocumentWithSignature(SignResponse signResponse)
        {
            if (null == signResponse.OptionalOutputs)
            {
                return null;
            }
            foreach (XmlElement optionalOutput in signResponse.OptionalOutputs.Any)
            {
                if (optionalOutput.NamespaceURI.Equals(DSSConstants.DSS_NAMESPACE) &&
                    optionalOutput.LocalName.Equals("DocumentWithSignature"))
                {
                    DSSXSDNamespace.DocumentWithSignature documentWithSignature = (DSSXSDNamespace.DocumentWithSignature)
                        FromDom("DocumentWithSignature", DSSConstants.DSS_NAMESPACE, optionalOutput,
                        typeof(DSSXSDNamespace.DocumentWithSignature));
                    return documentWithSignature;
                }
            }

            return null;
        }

        private DSSXSDNamespace.StorageInfo FindStorageInfo(SignResponse signResponse)
        {
            if (null == signResponse.OptionalOutputs)
            {
                return null;
            }
            foreach (XmlElement optionalOutput in signResponse.OptionalOutputs.Any)
            {
                if (optionalOutput.NamespaceURI.Equals(DSSConstants.ARTIFACT_NAMESPACE) &&
                    optionalOutput.LocalName.Equals("StorageInfo"))
                {
                    DSSXSDNamespace.StorageInfo storageInfo = (DSSXSDNamespace.StorageInfo)FromDom("StorageInfo",
                        DSSConstants.ARTIFACT_NAMESPACE, optionalOutput, typeof(DSSXSDNamespace.StorageInfo));
                    return storageInfo;
                }
            }

            return null;
        }

        private InputDocuments GetInputDocuments(byte[] documentData, String mimeType)
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

        private void CheckResponse(ResponseBaseType response, String requestId)
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

        private String ValidateResult(ResponseBaseType response)
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
                throw new DSSRequestFailedException("unsuccessful result",resultMajor, resultMinor);
            }
            return resultMinor;
        }

        private XmlElement GetElement(String prefix, String elementName, String ns)
        {
            XmlDocument xmlDocument = new XmlDocument();
            return xmlDocument.CreateElement(prefix, elementName, ns);
        }

        private XmlElement ToDom(String elementName, String ns, Object o, Type type)
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

            return (XmlElement)xmlDocument.ChildNodes.Item(1);
        }

        private Object FromDom(String elementName, String ns, XmlNode xmlNode, Type type)
        {
            XmlRootAttribute xRoot = new XmlRootAttribute();
            xRoot.ElementName = elementName;
            xRoot.Namespace = ns;

            XmlSerializer serializer = new XmlSerializer(type, xRoot);

            XmlReader xmlReader = new XmlNodeReader(xmlNode);

            return serializer.Deserialize(xmlReader);
        }
    }
}
