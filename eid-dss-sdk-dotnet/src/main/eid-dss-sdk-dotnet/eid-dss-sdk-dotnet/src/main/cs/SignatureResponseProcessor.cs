using System;
using System.Collections.Generic;
using System.Text;
using System.Web;

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
        /// <exception cref="SignatureResponseProcessorException">something went wrong...</exception>
        public SignatureReponse process(HttpRequest request, String target,
                String base64EncodedSignatureRequest, String signatureRequestId,
                String relayState)
        {
            /*
             * Decode all incoming parameters
             */
            String signatureStatus = getParameter(request, SIGNATURE_STATUS_PARAM);
            if (null == signatureStatus)
            {
                throw new SignatureResponseProcessorException(SIGNATURE_STATUS_PARAM +
                    " parameter not present");
            }
            Console.WriteLine("Signature status: " + signatureStatus);
            return new SignatureReponse(null, null, null);
        }

        private String getParameter(HttpRequest request, String name)
        {
            String[] values = request.Form.GetValues(name);
            if (null == values || values.Length < 1) return null;
            return values[0];
        }
    }
}
