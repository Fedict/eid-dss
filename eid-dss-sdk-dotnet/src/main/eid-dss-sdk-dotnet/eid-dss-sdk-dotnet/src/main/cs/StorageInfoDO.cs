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

        public String getArtifact()
        {
            return this.artifact;
        }

        public DateTime getNotBefore()
        {
            return this.notBefore;
        }

        public DateTime getNotAfter()
        {
            return this.notAfter;
        }
    }
}
