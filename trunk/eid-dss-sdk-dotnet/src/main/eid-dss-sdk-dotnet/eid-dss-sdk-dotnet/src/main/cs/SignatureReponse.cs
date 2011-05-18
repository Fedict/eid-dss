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

        public byte[] DecodedSignatureResponse
        {
            get
            {
                return this.decodedSignatureResponse;
            }
            set
            {
                this.decodedSignatureResponse = value;
            }
        }

        public String SignatureResponseId
        {
            get
            {
                return this.signatureResponseId;
            }
            set
            {
                this.signatureResponseId = value;
            }
        }

        public X509Certificate SignatureCertificate
        {
            get
            {
                return this.signatureCertificate;
            }
            set
            {
                this.signatureCertificate = value;
            }
        }
    }
}
