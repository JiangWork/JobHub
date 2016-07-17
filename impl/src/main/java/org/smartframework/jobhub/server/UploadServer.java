package org.smartframework.jobhub.server;


import org.apache.log4j.Logger;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 *  A server accepts uploaded files.
 *  
 * @author Miller
 * @date Jul 2, 2016 2:37:38 PM
 */
public class UploadServer {
	private static final Logger logger = Logger.getLogger(UploadServer.class);
	
	private ServerBootstrap bootstrap;
	private String uploadDirectory = System.getProperty("java.tmp.dir", "/tmp") ;
	private int port;
	private Thread thread;
	
	
	public UploadServer(int port) {
		this.port = port;
	}
	/**
	 * Start the server in non-blocking fashion.
	 *
	 */
	public void start() throws Exception {
		// Configure the server.
		EventLoopGroup bossGroup = new NioEventLoopGroup(1);
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		
		bootstrap = new ServerBootstrap();
		bootstrap.group(bossGroup, workerGroup)
		.channel(NioServerSocketChannel.class)
		.option(ChannelOption.SO_BACKLOG, 100)
		.handler(new LoggingHandler(LogLevel.INFO))
		.childHandler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ChannelPipeline p = ch.pipeline();
				UploadServerHandler handler = new UploadServerHandler();
				handler.setUploadDirectory(uploadDirectory);
				p.addLast(handler);
			}
		});
		logger.info("Starting UploadServer localhost@" + port);
		offerService();
	}
	
	private void offerService() throws InterruptedException {
		// Start the server.
		thread = new Thread() {
			public void run() {
				setName("UploadServer");
				ChannelFuture f;
				try {
					f = bootstrap.bind(port).sync();
					f.channel().closeFuture().sync();
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		};
		thread.start();
	}
	
	
	public void stop() {
		if (bootstrap == null) {
			logger.info("server hasn't been startup.");
			return;
		}
		bootstrap.group().shutdownGracefully().syncUninterruptibly();
		bootstrap.childGroup().shutdownGracefully().syncUninterruptibly();
		try {
			thread.join();
		} catch (InterruptedException e) {
			//ignored.
		}
		logger.info("UploadServer is stopped successfully localhost@" + ServerContext.UPLOAD_PROTOCOL_PORT);
	}
	
	
	public String getUploadDirectory() {
		return uploadDirectory;
	}

	public void setUploadDirectory(String uploadDirectory) {
		this.uploadDirectory = uploadDirectory;
	}

	public static void main(String[] args) throws Exception {
		final UploadServer server = new UploadServer(32102);
		server.start();

//		Thread.sleep(10*1000);
//		server.close();
	}


}
