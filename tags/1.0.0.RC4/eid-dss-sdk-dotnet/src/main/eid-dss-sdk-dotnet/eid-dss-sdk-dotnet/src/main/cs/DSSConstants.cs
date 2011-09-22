using System;
using System.Collections.Generic;
using System.Text;

namespace eid_dss_sdk_dotnet
{
    public class DSSConstants
    {
        private DSSConstants()
        {
        }

        // OASIS DSS Result codes
        public static String RESULT_MAJOR_SUCCESS = "urn:oasis:names:tc:dss:1.0:resultmajor:Success";
        public static String RESULT_MAJOR_REQUESTER_ERROR = "urn:oasis:names:tc:dss:1.0:resultmajor:RequesterError";

        public static String RESULT_MINOR_VALID_SIGNATURE = "urn:oasis:names:tc:dss:1.0:resultminor:valid:signature:OnAllDocuments";
        public static String RESULT_MINOR_VALID_MULTI_SIGNATURES = "urn:oasis:names:tc:dss:1.0:resultminor:ValidMultiSignatures";
        public static String RESULT_MINOR_INVALID_SIGNATURE = "urn:oasis:names:tc:dss:1.0:resultminor:IncorrectSignature";
        public static String RESULT_MINOR_NOT_PARSEABLE_XML_DOCUMENT = "urn:oasis:names:tc:dss:1.0:resultminor:NotParseableXMLDocument";
        public static String RESULT_MINOR_NOT_SUPPORTED = "urn:oasis:names:tc:dss:1.0:resultminor:NotSupported";

        // namespaces
        public static String DSS_NAMESPACE = "urn:oasis:names:tc:dss:1.0:core:schema";
        public static String VR_NAMESPACE = "urn:oasis:names:tc:dss-x:1.0:profiles:verificationreport:schema#";
        public static String ARTIFACT_NAMESPACE = "be:fedict:eid:dss:profile:artifact-binding:1.0";
    }
}
