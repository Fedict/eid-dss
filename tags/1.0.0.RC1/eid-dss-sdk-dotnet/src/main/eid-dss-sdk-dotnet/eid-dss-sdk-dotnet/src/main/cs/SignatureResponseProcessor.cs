using System;
using System.Collections.Generic;
using System.Text;
using System.Web;
using System.Security.Cryptography.X509Certificates;
using System.Security.Cryptography.Xml;
using System.Security.Cryptography;
using Org.BouncyCastle.Utilities;

namespace eid_dss_sdk_dotnet
{
    public class SignatureResponseProcessor
    {
        /*
         * Response POST parameter names
         */
        private static String SIGNATURE_RESPONSE_PARAM = "SignatureResponse";
        private static String SIGNATURE_RESPONSE_ID_PARAM = "SignatureResponseId";
        private static String SIGNATURE_STATUS_PARAM = "SignatureStatus";
        private static String SIGNATURE_CERTIFICATE_PARAM = "SignatureCertificate";
        private static String RELAY_STATE_PARAM = "RelayState";

        private static String SERVICE_SIGNED_PARAM = "ServiceSigned";
        private static String SERVICE_SIGNATURE_PARAM = "ServiceSignature";
        private static String SERVICE_CERTIFICATE_CHAIN_SIZE_PARAM = "ServiceCertificateChainSize";
        private static String SERVICE_CERTIFICATE_PARAM_PREFIX = "ServiceCertificate.";

        private byte[] serviceFingerprint;

        public SignatureResponseProcessor(byte[] serviceFingerprint)
        {
            this.serviceFingerprint = serviceFingerprint;
        }

        /// <summary>
        /// Process the incoming DSS response.
        /// </summary>
        /// <param name="request">HTTP Request that holds the DSS response.</param>
        /// <param name="target">our target URL used for validation of the service signature.</param>
        /// <param name="base64EncodedSignatureRequest">optional base64 encoded signature request used
        /// for validation of the service signature. If <code>null</code> meaning artifact binding was used,
        /// signatureRequestId becomes required.</param>
        /// <param name="signatureRequestId">optional signature request ID case artifact binding was used.</param>
        /// <param name="relayState">optiona relay state</param>
        /// <returns>the signature response DTO</returns>
        /// <exception cref="UserCancelledSignatureResponseProcessorException">User cancelled the signature ceremony.</exception>
        /// <exception cref="SignatureResponseProcessorException">something went wrong...</exception>
        public SignatureReponse Process(HttpRequest request, String target,
                String base64EncodedSignatureRequest, String signatureRequestId,
                String relayState)
        {
            /*
             * Decode all incoming parameters
             */
            String signatureStatus = GetParameter(request, SIGNATURE_STATUS_PARAM);
            if (null == signatureStatus)
            {
                throw new SignatureResponseProcessorException(SIGNATURE_STATUS_PARAM +
                    " parameter not present");
            }
            Console.WriteLine("Signature status: " + signatureStatus);

            if (!"OK".Equals(signatureStatus))
            {
                if ("USER_CANCELLED".Equals(signatureStatus))
                {
                    throw new UserCancelledSignatureResponseProcessorException("User cancelled");
                }
                throw new SignatureResponseProcessorException("Invalid signature status: " + signatureStatus);
            }

            String signatureResponse = GetParameter(request, SIGNATURE_RESPONSE_PARAM);
            String signatureResponseId = GetParameter(request, SIGNATURE_RESPONSE_ID_PARAM);

            if (null == signatureResponse && null == signatureResponseId)
            {
                throw new SignatureResponseProcessorException("No " + SIGNATURE_RESPONSE_PARAM +
                 " or " + SIGNATURE_RESPONSE_ID_PARAM + " parameter found!");
            }

            String encodedSignatureCertificate = GetParameter(request, SIGNATURE_CERTIFICATE_PARAM);
            if (null == encodedSignatureCertificate)
            {
                throw new SignatureResponseProcessorException("No " + SIGNATURE_CERTIFICATE_PARAM + " parameter found!");
            }

            /*
             * Validate RelayState if needed.
             */
            String responseRelayState = GetParameter(request, RELAY_STATE_PARAM);
            if (null != relayState)
            {
                if (!relayState.Equals(responseRelayState))
                {
                    throw new SignatureResponseProcessorException("Returned RelayState \"" + responseRelayState
                        + "\" does not match expected RelayState \"" + relayState + "\"");
                }
            }

            /*
             * Check service signature
             */
            String encodedServiceSigned = GetParameter(request, SERVICE_SIGNED_PARAM);
            if (null != encodedServiceSigned)
            {
                String serviceSigned = HttpUtility.UrlDecode(encodedServiceSigned);

                String encodedServiceSignature = GetParameter(request, SERVICE_SIGNATURE_PARAM);
                if (null == encodedServiceSignature)
                {
                    throw new SignatureResponseProcessorException("Missing " + SERVICE_SIGNATURE_PARAM + " parameter!");
                }
                byte[] serviceSignatureValue = Convert.FromBase64String(encodedServiceSignature);

                /*
                 * Parse the service certificate chain
                 */
                String serviceCertificateChainSizeString = GetParameter(request, SERVICE_CERTIFICATE_CHAIN_SIZE_PARAM);
                if (null == serviceCertificateChainSizeString)
                {
                    throw new SignatureResponseProcessorException("Missing " + SERVICE_CERTIFICATE_CHAIN_SIZE_PARAM + " parameter!");
                }
                int serviceCertificateChainSize = Int32.Parse(serviceCertificateChainSizeString);
                List<X509Certificate2> serviceCertificateChain = new List<X509Certificate2>();
                for (int idx = 1; idx <= serviceCertificateChainSize; idx++)
                {
                    String encodedCertificate = GetParameter(request, SERVICE_CERTIFICATE_PARAM_PREFIX + idx);
                    byte[] certificateData = Convert.FromBase64String(encodedCertificate);
                    X509Certificate2 certificate = new X509Certificate2(certificateData);
                    serviceCertificateChain.Add(certificate);
                }

                if (null == target)
                {
                    throw new SignatureResponseProcessorException("\"target\" parameter required for validation of service signature");
                }

                if (null == base64EncodedSignatureRequest && null == signatureRequestId)
                {
                    throw new SignatureResponseProcessorException("SignatureRequest or SignatureRequestId parameter required " +
                        "for validation of service signature");
                }

                VerifyServiceSignature(serviceSigned, target, base64EncodedSignatureRequest, signatureRequestId,
                    signatureResponse, signatureResponseId, encodedSignatureCertificate, serviceSignatureValue, serviceCertificateChain);
            }
            else
            {
                if (null != this.serviceFingerprint)
                {
                    throw new SignatureResponseProcessorException("Service fingerprint available but service signature is missing.");
                }
            }

            /*
             * Parse all incoming data
             */
            byte[] decodedSignatureResponse = null;
            if (null != signatureResponse)
            {
                decodedSignatureResponse = Convert.FromBase64String(signatureResponse);
            }

            byte[] decodedSignatureCertificate = Convert.FromBase64String(encodedSignatureCertificate);
            X509Certificate signatureCertificate = new X509Certificate(decodedSignatureCertificate);

            return new SignatureReponse(decodedSignatureResponse, signatureResponseId, signatureCertificate);
        }

        private void VerifyServiceSignature(String serviceSigned, String target, String signatureRequest,
            String signatureRequestId, String signatureResponse, String signatureResponseId, String encodedSignatureCertificate,
            byte[] serviceSignatureValue, List<X509Certificate2> serviceCertificateChain)
        {

            X509Certificate2 serviceCertificate = serviceCertificateChain[0];

            RSACryptoServiceProvider rsa = (RSACryptoServiceProvider)serviceCertificate.PublicKey.Key;

            List<byte> signatureData = new List<byte>();

            foreach (String serviceSignedElement in serviceSigned.Split(','))
            {
                if ("target".Equals(serviceSignedElement))
                {
                    signatureData.AddRange(ToByteArray(target));
                }
                else if ("SignatureRequest".Equals(serviceSignedElement))
                {
                    signatureData.AddRange(ToByteArray(signatureRequest));
                }
                else if ("SignatureRequestId".Equals(serviceSignedElement))
                {
                    signatureData.AddRange(ToByteArray(signatureRequestId));
                }
                else if ("SignatureResponse".Equals(serviceSignedElement))
                {
                    signatureData.AddRange(ToByteArray(signatureResponse));
                }
                else if ("SignatureResponseId".Equals(serviceSignedElement))
                {
                    signatureData.AddRange(ToByteArray(signatureResponseId));
                }
                else if ("SignatureCertificate".Equals(serviceSignedElement))
                {
                    signatureData.AddRange(ToByteArray(encodedSignatureCertificate));
                }
            }

            bool valid = rsa.VerifyData(signatureData.ToArray(), "SHA1", serviceSignatureValue);
            if (!valid)
            {
                throw new SystemException("Service signature not valid!");
            }

            //  verify service fingerprint if configured
            if (null != this.serviceFingerprint)
            {
                byte[] actualServiceFingerprint = SHA1.Create().ComputeHash(serviceCertificate.GetRawCertData());
                if (!Arrays.AreEqual(this.serviceFingerprint, actualServiceFingerprint))
                {
                    throw new SystemException("Service certificate fingerprint mismatch!");
                }
            }
        }


        public static byte[] ToByteArray(string str)
        {
            System.Text.UTF8Encoding encoding = new System.Text.UTF8Encoding();
            return encoding.GetBytes(str);
        }


        private String GetParameter(HttpRequest request, String name)
        {
            String[] values = request.Form.GetValues(name);
            if (null == values || values.Length < 1) return null;
            return values[0];
        }
    }
}
