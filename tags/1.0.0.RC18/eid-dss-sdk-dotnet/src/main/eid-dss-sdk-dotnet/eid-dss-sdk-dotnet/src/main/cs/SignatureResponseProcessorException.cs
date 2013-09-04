using System;
using System.Collections.Generic;
using System.Text;

namespace eid_dss_sdk_dotnet
{
    public class SignatureResponseProcessorException : Exception
    {
        public SignatureResponseProcessorException(String message) : base(message)
        {
        }
    }
}
