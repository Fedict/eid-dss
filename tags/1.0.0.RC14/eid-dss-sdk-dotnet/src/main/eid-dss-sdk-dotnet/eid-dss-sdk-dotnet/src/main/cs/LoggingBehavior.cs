using System;
using System.ServiceModel;
using System.ServiceModel.Channels;
using System.ServiceModel.Description;
using System.ServiceModel.Dispatcher;

namespace eid_dss_sdk_dotnet
{
	/// <summary>
	/// LoggingBehavior
	/// 
	/// This custom behavior makes sure the SOAP message is logged to the console before it is sent.
	/// </summary>
	public class LoggingBehavior : IEndpointBehavior {
		
		public void Validate(ServiceEndpoint endpoint)
		{
		}
		
		public void AddBindingParameters(ServiceEndpoint endpoint, BindingParameterCollection bindingParameters)
		{
		}
		
		public void ApplyClientBehavior(ServiceEndpoint endpoint, ClientRuntime clientRuntime)
		{
			clientRuntime.MessageInspectors.Add(new LoggingClientMessageInspector());
		}
		
		public void ApplyDispatchBehavior(ServiceEndpoint endpoint, EndpointDispatcher endpointDispatcher)
		{
			throw new NotImplementedException();
		}
	}

	/// <summary>
	/// LoggingClientMessageInspector
	/// 
	/// This is the client inspector class that is used to intercept the message on the client side.
	/// Before a request is made to the server, the message will be logged to the Console.
	/// </summary>
	public class LoggingClientMessageInspector : IClientMessageInspector
	{
		public object BeforeSendRequest(ref Message request, IClientChannel channel)
		{
			Console.WriteLine(request);
			return null;
		}
		
		public void AfterReceiveReply(ref Message reply, object correlationState)
		{
			Console.WriteLine(reply);
		}
	}
}
