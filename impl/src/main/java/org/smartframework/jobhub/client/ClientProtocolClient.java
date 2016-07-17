package org.smartframework.jobhub.client;

import org.apache.log4j.Logger;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.smartframework.jobhub.protocol.ClientProtocol;

public class ClientProtocolClient {
	
	private static final Logger logger = Logger.getLogger(ClientProtocolClient.class);

	public static ClientProtocol.Client newClient(String hostName, int port) {
		ClientProtocol.Client client = null;
		for (int i = 0; i < 3; ++i) {
			try {
				TTransport transport = new TSocket(hostName, port);
				transport.open();
				TProtocol protocol = new TBinaryProtocol(transport);
				client = new ClientProtocol.Client(protocol);
				break;
			} catch (TTransportException e) {
				logger.error(
						"Fail to obtain client at try " + i + ": " + e.getMessage(), e);
			}
		}
		return client;
	}
	
	public static void close(ClientProtocol.Client client) {
		client.getInputProtocol().getTransport().close();
		client.getOutputProtocol().getTransport().close();
	}
}
