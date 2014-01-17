using System;
using System.Collections.Generic;
using System.Text;

namespace eid_dss_sdk_dotnet
{
    public class UserCancelledSignatureResponseProcessorException : SignatureResponseProcessorException
    {
        public UserCancelledSignatureResponseProcessorException(String message)
            : base(message)
        {
        }
    }
}
