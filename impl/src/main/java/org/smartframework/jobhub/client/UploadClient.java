package org.smartframework.jobhub.client;

import org.apache.log4j.Logger;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * A client sends file to server.
 * 
 * @author Miller
 * @date Jul 2, 2016 3:07:33 PM
 */
public class UploadClient {
	
	private static final Logger logger = Logger.getLogger(UploadClient.class);
	
	private String host;
	private int port;
	private String reply;
	
	public UploadClient(String host, int port) {
		this.host = host;
		this.port = port;
	}
	
	//"/Users/Miller/Downloads/netty-4.0.36.Final/jar/all-in-one/netty-all-4.0.36.Final.jar"
	public boolean upload(String filePath, long jobId) throws InterruptedException {
    	final UploadClientHandler handler = new UploadClientHandler(filePath, jobId);
        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .option(ChannelOption.TCP_NODELAY, true)
             .handler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel ch) throws Exception {
                     ChannelPipeline p = ch.pipeline();
                     p.addLast(handler);
                 }
             });

            // Start the client.
            ChannelFuture f = b.connect(host, port).sync();
            // Wait until the connection is closed.
            f.channel().closeFuture().syncUninterruptibly();
            logger.info("status:" + handler.isSuccess() + " reply:" + handler.getReply());
            reply = handler.getReply();
            return handler.isSuccess();
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
        }
	}
	
    public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getReply() {
		return reply;
	}

	public static void main(String[] args) throws Exception {
		UploadClient client = new UploadClient("localhost", 32102);
		boolean status = client.upload("/Users/Miller/Downloads/Casual S01E08 720p I Amsterdam.mp4", 1);
		System.out.println(status);
		System.out.println(client.reply);
		status = client.upload("/Users/Miller/Downloads/progit-en.1084.pdf", 1);
		System.out.println(status);
		System.out.println(client.reply);
    }

}
