using System;
using System.Collections.Generic;
using System.Text;
using System.Security.Cryptography.X509Certificates;

namespace eid_dss_sdk_dotnet
{
    public class SignatureInfo
    {
        private X509Certificate signer;
        private DateTime signingTime;
        private String role;

        public SignatureInfo(X509Certificate signer, DateTime signingTime, String role)
        {
            this.signer = signer;
            this.signingTime = signingTime;
            this.role = role;
        }

        public X509Certificate Signer
        {
            get
            {
                return this.signer;
            }
            set
            {
                this.signer = value;
            }
        }

        public DateTime SigningTime
        {
            get
            {
                return this.signingTime;
            }
            set
            {
                this.signingTime = value;
            }
        }

        public String Role
        {
            get
            {
                return this.role;
            }
            set
            {
                this.role = value;
            }
        }
    }
}
