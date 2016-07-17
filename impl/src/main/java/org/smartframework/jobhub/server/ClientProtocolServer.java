package org.smartframework.jobhub.server;

import org.apache.log4j.Logger;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.smartframework.jobhub.protocol.ClientProtocol;

/**
 * A client protocol server handles user's job submission,
 * query requests.
 * 
 * @author Miller
 * @date Jul 2, 2016 9:55:08 PM
 */
public class ClientProtocolServer {
	
	private static final Logger logger = Logger.getLogger(ClientProtocolServer.class);
	
	private TServer tserver;
	private Thread thread;
	
	private ServerContext ctx;
	
	public ClientProtocolServer(ServerContext ctx) {
		this.ctx = ctx;
	}
	
	/**
	 * Start the server in non-blocking fashion.
	 * @throws TTransportException
	 */
	public void start() throws TTransportException {
		ClientProtocolImpl impl = new ClientProtocolImpl(ctx.getJobManager());
		TServerTransport serverTransport = new TServerSocket(ServerContext.CLIENT_PROTOCOL_PORT);
		TProcessor processor = new ClientProtocol.Processor<ClientProtocol.Iface>(impl);
		tserver = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));
		logger.info("Starting ClientProtocolServer localhost@" + ServerContext.CLIENT_PROTOCOL_PORT);
		offerService(tserver);
	}
	
	public void offerService(final TServer tserver) {
		thread = new Thread() {
			public void run() {
				this.setName("ClientProtocolServerThread");
				tserver.serve();
			}
		};
		thread.start();
	}
	
	public void stop() {
		while(tserver.isServing()) tserver.stop();
		try {
			thread.join();
		} catch (InterruptedException e) {
			//ignored
		}
		logger.info("ClientProtocolServer is stopped successfully localhost@" + ServerContext.CLIENT_PROTOCOL_PORT);
	}
	
	public void waitForServing() {
		while(!tserver.isServing());
	}
}
