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
        private byte[] invalidSignedDocument;

        [SetUp]
        public void setup()
        {
            // read valid signed document
            FileStream fs = new FileStream(DOC_DIRECTORY_PATH + "doc-signed.xml", 
                FileMode.Open, FileAccess.Read);
            validSignedDocument = new byte[fs.Length];
            fs.Read(validSignedDocument, 0, System.Convert.ToInt32(fs.Length));
            fs.Close();

            // read invalid signed document
            fs = new FileStream(DOC_DIRECTORY_PATH + "doc-invalid-signed.xml",
                FileMode.Open, FileAccess.Read);
            invalidSignedDocument = new byte[fs.Length];
            fs.Read(invalidSignedDocument, 0, System.Convert.ToInt32(fs.Length));
            fs.Close();
        }

        [Test]
        public void TestVerifyValidDocument()
        {
            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(DSS_LOCATION);

            bool result = client.verify(validSignedDocument, "text/xml");
            Assert.True(result);
        }

        [Test]
        public void TestVerifyInvalidSignatureDocument()
        {
            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(DSS_LOCATION);

            try
            {
                client.verify(invalidSignedDocument, "text/xml");
                Assert.Fail();
            }
            catch (SystemException e)
            {
                // expected
                Console.WriteLine("SystemException: " + e.Message);
            }
        }

        [Test]
        public void TestVerifyValidDocumentSslNoTlsAuthn()
        {
            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(DSS_LOCATION_SSL);
            client.configureSsl(null);

            bool result = client.verify(validSignedDocument, "text/xml");
            Assert.True(result);
        }

        [Test]
        public void TestVerifyValidDocumentValidTlsAuthn()
        {
            X509Certificate2 sslCertificate = new X509Certificate2(SSL_CERT_PATH);

            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(DSS_LOCATION_SSL);
            client.configureSsl(sslCertificate);
            
            bool result = client.verify(validSignedDocument, "text/xml");
            Assert.True(result);
        }

        [Test]
        public void TestVerifyValidDocumentInvalidTlsAuthn()
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
                Console.WriteLine("SystemException: " + e.Message);
            }
        }

        [Test]
        public void TestVerifyWithSignersValidDocument()
        {
            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(DSS_LOCATION);

            List<SignatureInfo> signers = client.verifyWithSigners(validSignedDocument, "text/xml");
            Assert.NotNull(signers);

            foreach (SignatureInfo signer in signers)
            {
                Console.WriteLine("------------------------------------------");
                Console.WriteLine("Signer: " + signer.getSigner().Subject);
                Console.WriteLine("Signing Time: " + signer.getSigningTime());
                Console.WriteLine("Role: " + signer.getRole());
                Console.WriteLine("------------------------------------------");
            }
        }

    }
}