using System;
using System.Collections.Generic;
using System.Text;

namespace eid_dss_sdk_dotnet
{
    public class DSSRequestFailedException : Exception
    {
        private string resultMajor;
        private string resultMinor;

        public DSSRequestFailedException(string message, string resultMajor, string resultMinor)
            : base(message)
        {
            this.resultMajor = resultMajor;
            this.resultMinor = resultMinor;
        }

        public String ResultMajor
        {
            get { return this.resultMajor; }
            set { this.resultMajor = value; }
        }
        public String ResultMinor
        {
            get { return this.resultMinor; }
            set { this.resultMinor = value; }
        }
    }
}
