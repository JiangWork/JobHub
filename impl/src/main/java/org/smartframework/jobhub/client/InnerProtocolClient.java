package org.smartframework.jobhub.client;

import org.apache.log4j.Logger;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.smartframework.jobhub.protocol.InnerProtocol;

public class InnerProtocolClient {
	
	private static final Logger logger = Logger.getLogger(InnerProtocolClient.class);

	public static InnerProtocol.Client newClient(String hostName, int port) {
		InnerProtocol.Client client = null;
		for (int i = 0; i < 3; ++i) {
			try {
				TTransport transport = new TSocket(hostName, port);
				transport.open();
				TProtocol protocol = new TBinaryProtocol(transport);
				client = new InnerProtocol.Client(protocol);
				logger.info("Obtained client successfully " + client);
				break;
			} catch (TTransportException e) {
				logger.error(
						"Fail to obtain client at try " + i + ": " + e.getMessage(), e);
			}
		}
		return client;
	}
	
	public static void close(InnerProtocol.Client client) {
		client.getInputProtocol().getTransport().close();
		client.getOutputProtocol().getTransport().close();
	}
}
