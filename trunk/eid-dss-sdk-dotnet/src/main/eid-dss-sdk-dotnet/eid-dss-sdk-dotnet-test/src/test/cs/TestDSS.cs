using System;
using Org.BouncyCastle.X509;
using NUnit.Framework;
using eid_dss_sdk_dotnet;
using Org.BouncyCastle.Crypto;
using System.Collections.Generic;
using System.Security.Cryptography.X509Certificates;
using System.ServiceModel.Security;
using System.IO;
using System.Security.Cryptography;
using Org.BouncyCastle.Utilities;
using Org.BouncyCastle.Security;
using Org.BouncyCastle.Math;
using Org.BouncyCastle.Crypto.Parameters;
using Org.BouncyCastle.Crypto.Generators;
using System.Collections;
using Org.BouncyCastle.Asn1.X509;
using Org.BouncyCastle.Pkcs;

namespace eid_dss_sdk_dotnet.test.cs
{
    [TestFixture]
    public class TestDSS
    {
        public static string DSS_LOCATION_SSL = "https://sebeco-dev-11:8443/eid-dss-ws/dss";
        public static string DSS_LOCATION = "http://sebeco-dev-11:8080/eid-dss-ws/dss";

        public static string TEST_DIRECTORY_PATH = "C:\\Users\\devel\\Documents\\Test\\";

        public static string TEST_DOC_PATH = TEST_DIRECTORY_PATH + "documents\\";

        public static string CERT_DIRECTORY_PATH = TEST_DIRECTORY_PATH + "certificates\\";
        public static string SSL_CERT_PATH = CERT_DIRECTORY_PATH + "eiddss_ssl.cer";
        public static string INVALID_SSL_CERT_PATH = CERT_DIRECTORY_PATH + "invalid_ssl.cer";

        public static string TEST_PFX_PATH = CERT_DIRECTORY_PATH + "test.pfx";
        public static string TEST_PFX_PASSWORD = "secret";
        public static string TEST_CRT_PATH = CERT_DIRECTORY_PATH + "test.crt";

        private byte[] validSignedDocument;
        private byte[] invalidSignedDocument;
        private byte[] unsignedDocument;

        [SetUp]
        public void setup()
        {
            // read valid signed document
            FileStream fs = new FileStream(TEST_DOC_PATH + "doc-signed.xml",
                FileMode.Open, FileAccess.Read);
            validSignedDocument = new byte[fs.Length];
            fs.Read(validSignedDocument, 0, System.Convert.ToInt32(fs.Length));
            fs.Close();

            // read invalid signed document
            fs = new FileStream(TEST_DOC_PATH + "doc-invalid-signed.xml",
                FileMode.Open, FileAccess.Read);
            invalidSignedDocument = new byte[fs.Length];
            fs.Read(invalidSignedDocument, 0, System.Convert.ToInt32(fs.Length));
            fs.Close();

            // read unsigned document
            fs = new FileStream(TEST_DOC_PATH + "doc.xml",
                FileMode.Open, FileAccess.Read);
            unsignedDocument = new byte[fs.Length];
            fs.Read(unsignedDocument, 0, System.Convert.ToInt32(fs.Length));
            fs.Close();
        }

        [Test]
        public void TestVerifyValidDocument()
        {
            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(DSS_LOCATION);

            bool result = client.Verify(validSignedDocument, "text/xml");
            Assert.True(result);
        }

        [Test]
        public void TestVerifyInvalidSignatureDocument()
        {
            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(DSS_LOCATION);

            try
            {
                client.Verify(invalidSignedDocument, "text/xml");
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
            client.ConfigureSsl(null);

            bool result = client.Verify(validSignedDocument, "text/xml");
            Assert.True(result);
        }

        [Test]
        public void TestVerifyValidDocumentValidTlsAuthn()
        {
            X509Certificate2 sslCertificate = new X509Certificate2(SSL_CERT_PATH);

            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(DSS_LOCATION_SSL);
            client.ConfigureSsl(sslCertificate);

            bool result = client.Verify(validSignedDocument, "text/xml");
            Assert.True(result);
        }

        [Test]
        public void TestVerifyValidDocumentInvalidTlsAuthn()
        {
            X509Certificate2 invalidSslCertificate = new X509Certificate2(INVALID_SSL_CERT_PATH);
            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(DSS_LOCATION_SSL);
            client.ConfigureSsl(invalidSslCertificate);
            try
            {
                client.Verify(validSignedDocument, "text/xml");
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

            List<SignatureInfo> signers = client.VerifyWithSigners(validSignedDocument, "text/xml");
            Assert.NotNull(signers);

            foreach (SignatureInfo signer in signers)
            {
                Console.WriteLine("------------------------------------------");
                Console.WriteLine("Signer: " + signer.Signer.Subject);
                Console.WriteLine("Signing Time: " + signer.SigningTime);
                Console.WriteLine("Role: " + signer.Role);
                Console.WriteLine("------------------------------------------");
            }
        }

        [Test]
        public void TestDigest()
        {
            String serviceFingerprint = "96964dfed390fc3a884d897f00bc4446cb9d9429";
            String serviceCertificateString = "MIIB8zCCAVygAwIBAgIETSMjbDANBgkqhkiG9w0BAQUFADA+MQswCQYDVQQGEwJCRTEPMA0GA1UEChMGRmVkSUNUMQ8wDQYDVQQLEwZGZWRJQ1QxDTALBgNVBAMTBFRlc3QwHhcNMTEwMTA0MTM0MTAwWhcNMTEwNzAzMTM0MTAwWjA+MQswCQYDVQQGEwJCRTEPMA0GA1UEChMGRmVkSUNUMQ8wDQYDVQQLEwZGZWRJQ1QxDTALBgNVBAMTBFRlc3QwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAIWZDsOroiTb8rlDy6GoUhoG632DGSPjcvHHr/PVT1qsv7+goC6gUPo/4HHxSS67oJZxMABYYLFosBM/wtz5MIBlfCZYcxaVwhxd8HbWtzkBWvaZ9UobWoa83DL5ns1g4zOYkYA4KMBzDTP/s36dVT4vnB0WQvjqxHFtheoNacDNAgMBAAEwDQYJKoZIhvcNAQEFBQADgYEAecWebvuTk04zuYO7npHgpNi0IgmOafBW9mmeQBWq7gJlm5sy8nK/HJjtmRxjnRzo+iQ89On5Acipg5H0PIH5HVLf4zoLdH86tohzj0ohpw+rUma4aCwhyQfO+QqS2PokHM7elF0yUNYrZdoY3InoYuXvS1oejGeOJ6wiZ4dqN/c=";

            X509Certificate2 serviceCertificate = new X509Certificate2(Convert.FromBase64String(serviceCertificateString));
            Assert.NotNull(serviceCertificate);

            String resultServiceCertificateString = Convert.ToBase64String(serviceCertificate.GetRawCertData());
            Assert.True(serviceCertificateString.Equals(resultServiceCertificateString));

            byte[] actualServiceFingerprint = SHA1.Create().ComputeHash(serviceCertificate.GetRawCertData());
            String resultFingerprint = BitConverter.ToString(actualServiceFingerprint).Replace("-", "").ToLower();
            Console.WriteLine("result: " + resultFingerprint);
            Assert.True(serviceFingerprint.Equals(resultFingerprint));
        }

        [Test]
        public void TestStoreAndRetrieve()
        {
            DigitalSignatureServiceClient client = new DigitalSignatureServiceClientImpl(DSS_LOCATION);

            // store
            StorageInfoDO storageInfo = client.Store(unsignedDocument, "text/xml");
            Assert.NotNull(storageInfo);
            Assert.NotNull(storageInfo.Artifact);
            Assert.NotNull(storageInfo.NotBefore);
            Assert.NotNull(storageInfo.NotAfter);

            // verify store
            Console.WriteLine("Artifact: " + storageInfo.Artifact);
            Console.WriteLine("NotBefore: " + storageInfo.NotBefore);
            Console.WriteLine("NotAfter: " + storageInfo.NotAfter);

            // retrieve
            byte[] resultDocument = client.Retrieve(storageInfo.Artifact);

            // verify retrieve
            Assert.NotNull(resultDocument);
            Assert.True(Arrays.AreEqual(unsignedDocument, resultDocument));
        }

        [Test]
        public void TestCreateKeyStore()
        {
            AsymmetricCipherKeyPair keyPair = KeyStoreUtil.GenerateKeyPair();
            RsaPrivateCrtKeyParameters RSAprivKey = (RsaPrivateCrtKeyParameters)keyPair.Private;
            RsaKeyParameters RSApubKey = (RsaKeyParameters)keyPair.Public;

            Org.BouncyCastle.X509.X509Certificate cert = KeyStoreUtil.CreateCert("Test", RSApubKey, RSAprivKey);
            Console.WriteLine(cert.ToString());

            string pfxPath = TEST_PFX_PATH;
            if (File.Exists(pfxPath))
            {
                pfxPath += "_old";
                if (File.Exists(pfxPath))
                {
                    File.Delete(pfxPath);
                }
            }
            FileStream fs = new FileStream(pfxPath, FileMode.CreateNew);
            KeyStoreUtil.WritePkcs12(RSAprivKey, cert, TEST_PFX_PASSWORD, fs);
            fs.Close();

            string crtPath = TEST_CRT_PATH;
            if (File.Exists(crtPath))
            {
                crtPath += "_old";
                if (File.Exists(crtPath))
                {
                    File.Delete(crtPath);
                }
            }
            FileStream certFileStream = new FileStream(crtPath, FileMode.CreateNew);
            byte[] encodedCert = cert.GetEncoded();
            certFileStream.Write(encodedCert, 0, encodedCert.Length);
            certFileStream.Close();
        }

        [Test]
        public void TestLoadKeyFromPfx()
        {
            RSACryptoServiceProvider rsa = KeyStoreUtil.GetPrivateKeyFromPfx(TEST_PFX_PATH, TEST_PFX_PASSWORD, true);
            Console.WriteLine(rsa);
            Assert.NotNull(rsa);
        }

        [Test]
        public void TestLoadCertFromPfx()
        {
            X509Certificate2 certificate = KeyStoreUtil.GetCertificateFromPfx(TEST_PFX_PATH, TEST_PFX_PASSWORD, true);
            Console.WriteLine(certificate);
            Assert.NotNull(certificate);
        }

        [Test]
        public void TestCreateServiceSignature()
        {
            RSACryptoServiceProvider rsa = KeyStoreUtil.GetPrivateKeyFromPfx(TEST_PFX_PATH, TEST_PFX_PASSWORD, true);
            X509Certificate2 certificate = KeyStoreUtil.GetCertificateFromPfx(TEST_PFX_PATH, TEST_PFX_PASSWORD, true);
            List<X509Certificate2> certificateChain = new List<X509Certificate2>();
            certificateChain.Add(certificate);

            ServiceSignatureDO serviceSignature = SignatureRequestUtil.CreateServiceSignature(rsa, certificateChain, "signature-request", null,
                "target", "language", "content-type", "relay-state");
            Assert.NotNull(serviceSignature);

            Assert.NotNull(serviceSignature.ServiceSigned);
            Assert.NotNull(serviceSignature.ServiceSignature);
            Assert.NotNull(serviceSignature.ServiceCertificateChainSize);
            Assert.NotNull(serviceSignature.ServiceCertificates);
            Assert.True(serviceSignature.ServiceCertificates.Count == 1);

            Console.WriteLine("ServiceSignature");
            Console.WriteLine("----------------");
            Console.WriteLine("  * ServiceSigned   =" + serviceSignature.ServiceSigned);
            Console.WriteLine("  * ServiceSignature=" + serviceSignature.ServiceSignature);
        }

    }
}