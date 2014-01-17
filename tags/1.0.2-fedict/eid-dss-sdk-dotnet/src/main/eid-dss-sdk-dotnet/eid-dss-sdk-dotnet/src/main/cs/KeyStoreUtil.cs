using System;
using System.Collections.Generic;
using System.Text;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;
using System.Collections;
using Org.BouncyCastle.X509;
using Org.BouncyCastle.Asn1.X509;
using Org.BouncyCastle.Math;
using Org.BouncyCastle.Pkcs;
using Org.BouncyCastle.Security;
using Org.BouncyCastle.Crypto.Parameters;
using Org.BouncyCastle.Crypto.Generators;
using System.IO;
using Org.BouncyCastle.Crypto;

namespace eid_dss_sdk_dotnet
{
    public class KeyStoreUtil
    {
        public static RSACryptoServiceProvider GetPrivateKeyFromPfx(String pfxPath, String password, bool useMachineKeyStore)
        {
            if (useMachineKeyStore)
            {
                X509Certificate2 certificate = new X509Certificate2(pfxPath, password);
                return (RSACryptoServiceProvider)certificate.PrivateKey;
            }
            else
            {
                X509Certificate2 certificate = new X509Certificate2(pfxPath, password, X509KeyStorageFlags.MachineKeySet);
                return (RSACryptoServiceProvider)certificate.PrivateKey;
            }
        }


        public static X509Certificate2 GetCertificateFromPfx(string pfxPath, string pfxPassword, bool useMachineKeyStore)
        {
            if (useMachineKeyStore)
                return new X509Certificate2(pfxPath, pfxPassword, X509KeyStorageFlags.MachineKeySet);
            else
                return new X509Certificate2(pfxPath, pfxPassword);
        }

        public static void WritePkcs12(RsaPrivateCrtKeyParameters privKey, Org.BouncyCastle.X509.X509Certificate certificate,
            string password, Stream stream)
        {
            Pkcs12Store store = new Pkcs12Store();
            X509CertificateEntry[] chain = new X509CertificateEntry[1];
            chain[0] = new X509CertificateEntry(certificate);
            store.SetKeyEntry("privateKey", new AsymmetricKeyEntry(privKey), chain);
            store.Save(stream, password.ToCharArray(), new SecureRandom());
        }

        public static AsymmetricCipherKeyPair GenerateKeyPair()
        {
            SecureRandom sr = new SecureRandom();
            BigInteger pubExp = new BigInteger("10001", 16);
            RsaKeyGenerationParameters RSAKeyGenPara = new RsaKeyGenerationParameters(pubExp, sr, 1024, 80);
            RsaKeyPairGenerator RSAKeyPairGen = new RsaKeyPairGenerator();
            RSAKeyPairGen.Init(RSAKeyGenPara);
            return RSAKeyPairGen.GenerateKeyPair();
        }

        public static Org.BouncyCastle.X509.X509Certificate CreateCert(String cn,
            AsymmetricKeyParameter pubKey, AsymmetricKeyParameter privKey)
        {
            Hashtable attrs = new Hashtable();
            attrs.Add(X509Name.CN, cn);

            ArrayList ord = new ArrayList(attrs.Keys);

            X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();

            certGen.SetSerialNumber(BigInteger.One);
            certGen.SetIssuerDN(new X509Name(ord, attrs));
            certGen.SetNotBefore(DateTime.UtcNow.AddDays(-30));
            certGen.SetNotAfter(DateTime.UtcNow.AddDays(30));
            certGen.SetSubjectDN(new X509Name(ord, attrs));
            certGen.SetPublicKey(pubKey);
            certGen.SetSignatureAlgorithm("SHA1WithRSAEncryption");

            Org.BouncyCastle.X509.X509Certificate cert = certGen.Generate(privKey);

            cert.CheckValidity(DateTime.UtcNow);

            cert.Verify(pubKey);

            return cert;
        }

    }
}
