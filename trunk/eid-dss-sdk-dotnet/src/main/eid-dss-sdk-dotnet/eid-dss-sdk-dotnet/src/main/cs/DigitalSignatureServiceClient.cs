using System;
using System.Collections.Generic;
using System.Text;
using System.Security.Cryptography.X509Certificates;

namespace eid_dss_sdk_dotnet
{
    public interface DigitalSignatureServiceClient
    {
        /// <summary>
        /// Configure the certificate to validate the SSL certificate against.
        /// If not specified, any SSL certificate will be accepted. If this call is not made
        /// and you wish to contact the DSS WS over SSL, it will fail.
        /// </summary>
        /// <param name="sslCertificate">the SSL certificate to use for validation.
        /// To accept any, specify null here.</param>
        void configureSsl(X509Certificate2 sslCertificate);

        /// <summary>
        /// Set whether to log the outgoing/incoming SOAP messages. 
        /// If not explicitly specified, logging is enabled.
        /// </summary>
        /// <param name="logging">logging or not</param>
        void setLogging(bool logging);

        /// <summary>
        /// Verifies whether the given document has been signed or not.
        /// </summary>
        /// <param name="signedDocument">The signed document to verify</param>
        /// <param name="mimeType">optional mime-type, default is "text/xml"</param>
        /// <returns></returns>
        /// <exception cref="NotParseableXMLDocumentException">Document was not XML parseable.</exception>
        bool verify(byte[] signedDocument, String mimeType);

        /// <summary>
        /// Verifies whether the given document has been signed and reports back on the signing parties.
        /// </summary>
        /// <param name="signedDocument">The signed document to verify</param>
        /// <param name="mimeType">optional mime-type, default is "text/xml"</param>
        /// <returns>a list of signature information objects detailing on the signing parties.</returns>
        List<SignatureInfo> verifyWithSigners(byte[] signedDocument, String mimeType);
    }
}
