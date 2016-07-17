package org.smartframework.jobhub.server;

import org.apache.log4j.Logger;
import org.apache.thrift.TProcessor;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TTransportException;
import org.smartframework.jobhub.protocol.InnerProtocol;


/**
 * A InnerProtocolServer is used to handle job report requests.
 * 
 * @author Miller
 * @date Jul 2, 2016 10:34:17 PM
 */
public class InnerProtocolServer {
	private static final Logger logger = Logger.getLogger(InnerProtocolServer.class);
	
	private TServer tserver;
	private Thread thread;
	
	private ServerContext ctx;
	
	public InnerProtocolServer(ServerContext ctx) {
		this.ctx = ctx;
	}
	
	/**
	 * Start the server in non-blocking fashion.
	 * @throws TTransportException
	 */
	public void start() throws TTransportException {
		InnerProtocolImpl impl = new InnerProtocolImpl(ctx.getJobManager());
		TServerTransport serverTransport = new TServerSocket(ServerContext.INNER_PROTOCOL_PORT);
		TProcessor processor = new InnerProtocol.Processor<InnerProtocol.Iface>(impl);
		tserver = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));
		logger.info("Starting InnerProtocolServer localhost@" + ServerContext.INNER_PROTOCOL_PORT);
		offerService(tserver);
	}
	
	private void offerService(final TServer tserver) {
		thread = new Thread() {
			public void run() {
				this.setName("InnerProtocolServerThread");
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
		logger.info("InnerProtocolServer is stopped successfully localhost@" + ServerContext.INNER_PROTOCOL_PORT);
	}
	
	public void waitForServing() {
		while(!tserver.isServing());
	}


}
