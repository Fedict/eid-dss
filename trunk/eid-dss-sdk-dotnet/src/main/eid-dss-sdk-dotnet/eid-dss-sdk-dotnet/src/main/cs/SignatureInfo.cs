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

        public X509Certificate getSigner()
        {
            return this.signer;
        }

        public DateTime getSigningTime()
        {
            return this.signingTime;
        }

        public String getRole()
        {
            return this.role;
        }
    }
}
