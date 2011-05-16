using System;
using System.Collections.Generic;
using System.Text;
using System.Security.Cryptography.X509Certificates;

namespace eid_dss_sdk_dotnet
{
    public interface DigitalSignatureServiceClient
    {
        void configureSsl(X509Certificate2 sslCertificate);

        /// <summary>
        /// Verifies whether the given document has been signed or not.
        /// </summary>
        /// <param name="signedDocument">The signed document to verify</param>
        /// <param name="mimeType">optional mime-type, default is "text/xml"</param>
        /// <returns></returns>
        /// <exception cref="NotParseableXMLDocumentException">Document was not XML parseable.</exception>
        bool verify(byte[] signedDocument, String mimeType);
    }
}
