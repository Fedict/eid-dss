using System;
using System.Collections.Generic;
using System.Text;
using System.ServiceModel;
using System.Security.Cryptography.X509Certificates;
using System.Net.Security;

namespace eid_dss_sdk_dotnet
{
    /// <summary>
    /// WCFUtil.
    ///
    /// Utility class used by the WCF clients.
    /// </summary>
    public class WCFUtil
    {
        /// <summary>
        /// Certificate validation callback to accepts any SSL certificate.
        /// </summary>
        public static bool AnyCertificateValidationCallback(Object sender,
                    X509Certificate certificate, X509Chain chain, SslPolicyErrors sslPolicyErrors)
        {
            Console.WriteLine("Any Certificate Validation Callback");
            return true;
        }

        /// <summary>
        /// BasicHttpBinding with SSL Transport Security. Provides NO message integrity.
        /// </summary>
        /// <returns></returns>
        public static BasicHttpBinding BasicHttpOverSSLBinding(long maxReceivedMessageSize)
        {
            BasicHttpBinding binding = new BasicHttpBinding(BasicHttpSecurityMode.Transport);
            BasicHttpSecurity security = binding.Security;

            HttpTransportSecurity transportSecurity = security.Transport;
            transportSecurity.ClientCredentialType = HttpClientCredentialType.None;
            transportSecurity.ProxyCredentialType = HttpProxyCredentialType.None;
            transportSecurity.Realm = "";

            if (maxReceivedMessageSize > 0) binding.MaxReceivedMessageSize = maxReceivedMessageSize;
            return binding;
        }
    }
}
