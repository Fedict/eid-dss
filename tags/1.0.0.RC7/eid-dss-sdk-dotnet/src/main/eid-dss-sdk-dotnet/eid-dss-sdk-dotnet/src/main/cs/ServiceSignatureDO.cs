using System;
using System.Collections.Generic;
using System.Text;

namespace eid_dss_sdk_dotnet
{
    public class ServiceSignatureDO
    {
        private String serviceSigned;
        private String serviceSignature;
        private String serviceCertificateChainSize;
        private List<String> serviceCertificates;

        public ServiceSignatureDO(String serviceSigned, String serviceSignature,
            String serviceCertificateChainSize, List<String> serviceCertificates)
        {
            this.serviceSigned = serviceSigned;
            this.serviceSignature = serviceSignature;
            this.serviceCertificateChainSize = serviceCertificateChainSize;
            this.serviceCertificates = serviceCertificates;
        }

        public String ServiceSigned
        {
            get
            {
                return this.serviceSigned;
            }
            set
            {
                this.serviceSigned = value;
            }
        }

        public String ServiceSignature
        {
            get
            {
                return this.serviceSignature;
            }
            set
            {
                this.serviceSignature = value;
            }
        }

        public String ServiceCertificateChainSize
        {
            get
            {
                return this.serviceCertificateChainSize;
            }
            set
            {
                this.serviceCertificateChainSize = value;
            }
        }

        public List<String> ServiceCertificates
        {
            get
            {
                return this.serviceCertificates;
            }
            set
            {
                this.serviceCertificates = value;
            }
        }
    }
}
