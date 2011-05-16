using System;
using Org.BouncyCastle.X509;
using NUnit.Framework;
using eid_dss_sdk_dotnet;
using Org.BouncyCastle.Crypto;
using System.Collections.Generic;
using System.Security.Cryptography.X509Certificates;
using System.ServiceModel.Security;
using System.IO;

namespace eid_dss_sdk_dotnet.test.cs
{
    [TestFixture]
    public class TestDSS
    {
        public static string DSS_LOCATION_SSL = "https://sebeco-dev-11:8443/eid-dss-ws/dss";
        public static string DSS_LOCATION = "http://sebeco-dev-11:8080/eid-dss-ws/dss";
        
        public static string CERT_DIRECTORY_PATH = "C:\\Users\\devel\\certificates\\";
        public static string SSL_CERT_PATH = CERT_DIRECTORY_PATH + "eiddss_ssl.cer";
        public static string INVALID_SSL_CERT_PATH = CERT_DIRECTORY_PATH + "invalid_ssl.cer";

        public static string DOC_DIRECTORY_PATH = "C:\\Users\\devel\\Documents\\";

        private byte[] validSignedDocument;

        [SetUp]
        public void setup()
        {
            // read valid signed document
            FileStream fs = new FileStream(DOC_DIRECTORY_PATH + "doc-signed.xml", 
                FileMode.Open, FileAccess.Read);
            validSignedDocument = new byte[fs.Length];
            fs.Read(validSignedDocument, 0, System.Convert.ToInt32(fs.Length));
            fs.Close();
        }

        [Test]
        public void TestValidDocument()
        {
            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(DSS_LOCATION);

            bool result = client.verify(validSignedDocument, "text/xml");
            Assert.True(result);
        }

        [Test]
        public void TestValidDocumentSslNoTlsAuthn()
        {
            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(DSS_LOCATION_SSL);
            client.configureSsl(null);

            bool result = client.verify(validSignedDocument, "text/xml");
            Assert.True(result);
        }

        [Test]
        public void TestValidDocumentValidTlsAuthn()
        {
            X509Certificate2 sslCertificate = new X509Certificate2(SSL_CERT_PATH);

            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(DSS_LOCATION_SSL);
            client.configureSsl(sslCertificate);
            
            bool result = client.verify(validSignedDocument, "text/xml");
            Assert.True(result);
        }

        [Test]
        public void TestValidDocumentInvalidTlsAuthn()
        {
            X509Certificate2 invalidSslCertificate = new X509Certificate2(INVALID_SSL_CERT_PATH);
            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(DSS_LOCATION_SSL);
            client.configureSsl(invalidSslCertificate);
            try
            {
                client.verify(validSignedDocument, "text/xml");
                Assert.Fail();
            }
            catch (SecurityNegotiationException e)
            {
                // expected
            }
        }
    }
}