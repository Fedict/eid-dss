using System;
using System.Collections.Generic;
using System.Text;

namespace eid_dss_sdk_dotnet
{
    public class StorageInfoDO
    {
        private String artifact;
        private DateTime notBefore;
        private DateTime notAfter;

        public StorageInfoDO(String artifact, DateTime notBefore, DateTime notAfter)
        {
            this.artifact = artifact;
            this.notBefore = notBefore;
            this.notAfter = notAfter;
        }

        public String Artifact
        {
            get
            {
                return this.artifact;
            }
        }

        public DateTime NotBefore
        {
            get
            {
                return this.notBefore;
            }
            set
            {
                this.notBefore = value;
            }
        }

        public DateTime NotAfter
        {
            get
            {
                return this.notAfter;
            }
            set
            {
                this.notAfter = value;
            }
        }
    }
}
