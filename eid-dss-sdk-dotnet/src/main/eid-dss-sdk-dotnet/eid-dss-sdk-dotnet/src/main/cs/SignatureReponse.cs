using System;
using System.Collections.Generic;
using System.Text;
using System.Security.Cryptography.X509Certificates;

namespace eid_dss_sdk_dotnet
{
    public class SignatureReponse
    {
        private byte[] decodedSignatureResponse;
        private String signatureResponseId;
        private X509Certificate signatureCertificate;

        public SignatureReponse(byte[] decodedSignatureResponse,
            String signatureResponseId, X509Certificate signatureCertificate)
        {
            this.decodedSignatureResponse = decodedSignatureResponse;
            this.signatureResponseId = signatureResponseId;
            this.signatureCertificate = signatureCertificate;
        }

        public byte[] getDecodedSignatureResponse()
        {
            return this.decodedSignatureResponse;
        }

        public String getSignatureResponseId()
        {
            return this.signatureResponseId;
        }

        public X509Certificate getSignatureCertificate()
        {
            return this.signatureCertificate;
        }
    }
}
