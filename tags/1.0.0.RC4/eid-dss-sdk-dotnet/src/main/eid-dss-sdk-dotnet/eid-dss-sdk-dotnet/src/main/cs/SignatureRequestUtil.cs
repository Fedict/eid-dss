using System;
using System.Collections.Generic;
using System.Text;
using System.Security.Cryptography;
using System.Security.Cryptography.X509Certificates;

namespace eid_dss_sdk_dotnet
{
    public class SignatureRequestUtil
    {

        /// <summary>
        /// Constructs a DSS Simple Protocol service signature.
        /// </summary>
        /// <param name="spPrivateKey">the SP private key used for signing</param>
        /// <param name="signatureRequest">signature request, if <code>null</code> signatureRequestId needs to be specified.</param>
        /// <param name="signatureRequestId">signature request ID, if <code>null</code> signatureRequest needs to be specified.</param>
        /// <param name="target">required target</param>
        /// <param name="language">optional language param</param>
        /// <param name="contentType">optional document content type</param>
        /// <param name="relayState">optional relay state</param>
        /// <returns>service signature DO containing the signature value, service signed property indiciating which fields were signed,
        /// and the SP certificate chain</returns>
        public static ServiceSignatureDO CreateServiceSignature(RSACryptoServiceProvider spRSA,
            List<X509Certificate2> spCertificateChain, String signatureRequest, String signatureRequestId,
            String target, String language, String contentType, String relayState)
        {
            Console.WriteLine("get service signature");

            if (null == spRSA || null == spCertificateChain)
            {
                Console.WriteLine("No SP key or certificate chain specified, no signature created!");
                return null;
            }

            if (null == signatureRequest && null == signatureRequestId)
            {
                throw new SystemException("Either SignatureRequest or SignatureRequestId needs to be specified, aborting...");
            }

            // construct service signature
            List<byte> signatureData = new List<byte>();
            signatureData.AddRange(ToByteArray(target));

            if (null != signatureRequest) signatureData.AddRange(ToByteArray(signatureRequest));
            else signatureData.AddRange(ToByteArray(signatureRequestId));

            if (null != language) signatureData.AddRange(ToByteArray(language));
            if (null != contentType) signatureData.AddRange(ToByteArray(contentType));
            if (null != relayState) signatureData.AddRange(ToByteArray(relayState));

            byte[] serviceSignatureValue = spRSA.SignData(signatureData.ToArray(), "SHA1");

            String encodedServiceSignature = Convert.ToBase64String(serviceSignatureValue);

            // construct service signed
            String serviceSigned = "target";
            if (null != signatureRequest) serviceSigned += ",SignatureRequest";
            else serviceSigned += ",SignatureRequestId";

            if (null != language) serviceSigned += ",language";
            if (null != contentType) serviceSigned += ",ContentType";
            if (null != relayState) serviceSigned += ",RelayState";

            // construct service certificate chain
            String serviceCertificateChainSize = spCertificateChain.Count.ToString();
            List<String> serviceCertificates = new List<string>();
            foreach (X509Certificate2 certificate in spCertificateChain)
            {
                serviceCertificates.Add(Convert.ToBase64String(certificate.RawData));
            }

            return new ServiceSignatureDO(serviceSigned, encodedServiceSignature, serviceCertificateChainSize, serviceCertificates); ;
        }

        public static byte[] ToByteArray(string str)
        {
            System.Text.UTF8Encoding encoding = new System.Text.UTF8Encoding();
            return encoding.GetBytes(str);
        }

    }
}
