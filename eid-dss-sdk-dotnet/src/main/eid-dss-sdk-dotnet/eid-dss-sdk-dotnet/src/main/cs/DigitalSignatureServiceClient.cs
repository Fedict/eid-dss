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
        void ConfigureSsl(X509Certificate2 sslCertificate);

        /// <summary>
        /// Set whether to log the outgoing/incoming SOAP messages. 
        /// If not explicitly specified, logging is enabled.
        /// </summary>
        /// <param name="logging">logging or not</param>
        void SetLogging(bool logging);

        /// <summary>
        /// Set the WS endpoint's binding "MaxReceivedMessageSize" property, default is 65536
        /// </summary>
        /// <param name="size"></param>
        void SetMaxReceivedMessageSize(long size);

        /// <summary>
        /// Verifies whether the given document has been signed or not.
        /// </summary>
        /// <param name="signedDocument">The signed document to verify</param>
        /// <param name="mimeType">optional mime-type, default is "text/xml"</param>
        /// <returns></returns>
        /// <exception cref="DSSRequestFailedException">Request failed, exception will contain the result major/minor codes.</exception>
        /// <exception cref="NotParseableXMLDocumentException">Document was not XML parseable.</exception>
        bool Verify(byte[] signedDocument, String mimeType);

        /// <summary>
        /// Verifies whether the given document has been signed and reports back on the signing parties.
        /// </summary>
        /// <param name="signedDocument">The signed document to verify</param>
        /// <param name="mimeType">optional mime-type, default is "text/xml"</param>
        /// <returns>a list of signature information objects detailing on the signing parties.</returns>
        /// <exception cref="DSSRequestFailedException">Request failed, exception will contain the result major/minor codes.</exception>
        List<SignatureInfo> VerifyWithSigners(byte[] signedDocument, String mimeType);

        /// <summary>
        /// Send specified document to the eID DSS WS for temp storage. The WS will return a StorageInfoDO containing
        /// the artifact ID for the upload document plus info on expiration of it.
        /// </summary>
        /// <param name="documentData">document to be signed</param>
        /// <param name="contentType">content type of the document to be signed</param>
        /// <returns>storage information object</returns>
        /// <exception cref="DSSRequestFailedException">Request failed, exception will contain the result major/minor codes.</exception>
        StorageInfoDO Store(byte[] documentData, String contentType);

        /// <summary>
        /// Retrieve the document specified by the given ID from the eID DSS service.
        /// </summary>
        /// <param name="documentId">the ID of the document to fetch</param>
        /// <returns>the decoded document data</returns>
        /// <exception cref="DocumentNotFoundException">no document was returned</exception>
        /// <exception cref="DSSRequestFailedException">Request failed, exception will contain the result major/minor codes.</exception>
        byte[] Retrieve(String documentId);
    }
}
