package org.smartframework.jobhub.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * A handler sends the file data to server. 
 * @author Miller
 * @date Jul 2, 2016 2:55:40 PM
 */
public class UploadClientHandler extends ChannelInboundHandlerAdapter  {

	private static final Logger logger = Logger.getLogger(UploadClientHandler.class);
	public final static int CHUCK_SIZE = 2048;
	private String filePath;
	private ByteBuf replyBuf = Unpooled.buffer(CHUCK_SIZE);
	private String reply;
	private int replyLength = -1;
	private boolean success;
	private FileInputStream fis;
	private int writeBytes = 0;
	
	private long jobId;
	private ChannelHandlerContext ctx;
	
	public UploadClientHandler(String filePath, long jobId) {
		this.filePath = filePath;
		success = true;
		this.jobId = jobId;
	}
	
	@Override
	public void channelActive(ChannelHandlerContext ctx) throws InterruptedException, FileNotFoundException {
		File file = new File(filePath);
		if (!file.canRead()) {
			reply = "Can't read file " + filePath;
			success = false;
			ctx.close();
			return;
		}
		this.ctx = ctx;
		fis = new FileInputStream(file);
		String fileName = file.getName();
		long fileSize = file.length();
		ByteBuf content = ctx.alloc().buffer();
		byte[] fileNameBytes = fileName.getBytes();
		content.writeInt(8 + 4 +fileNameBytes.length+8);
		content.writeLong(jobId);
		content.writeInt(fileNameBytes.length);
		content.writeBytes(fileNameBytes);
		content.writeLong(fileSize);
		logger.debug("Sending header length: " + (4+fileNameBytes.length+8));
		ctx.writeAndFlush(content).addListener(trafficGenerator);
	}

	public void writeChunk() {
		try {
			byte[] buf = new byte[CHUCK_SIZE];
			ByteBuf content = Unpooled.buffer(CHUCK_SIZE);
			int length;
			if((length = fis.read(buf)) != -1) {
				content.writeBytes(buf, 0, length);
				writeBytes += length;
				ctx.writeAndFlush(content).addListener(trafficGenerator);
			} else {
				logger.info("file bytes written:" + writeBytes);
				fis.close();
			}
		} catch (Exception e) {
			success = false;
			reply = e.getMessage();
			logger.error(e.getMessage(), e);
			ctx.close();
		} 
	}
	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) {
		ByteBuf m = (ByteBuf) msg;
		replyBuf.writeBytes(m); // (2)
		m.release();

		if (replyLength == -1 && replyBuf.readableBytes() >= 4) { // (3)
			replyLength = replyBuf.readInt();
		}
		if (replyLength != -1 && replyBuf.readableBytes() >= replyLength) {
			byte[] buf = new byte[replyLength];
			replyBuf.readBytes(buf);
			reply = new String(buf);
			ctx.close();
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		// Close the connection when an exception is raised.
		success = false;
		reply = cause.getMessage();
		ctx.close();
	}
	

	public String getReply() {
		return reply;
	}

	public void setReply(String reply) {
		this.reply = reply;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}
	
	private final ChannelFutureListener trafficGenerator = new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) {
            if (future.isSuccess()) {
            	writeChunk();
            } else {
            	logger.error(future.cause().getMessage(), future.cause());
            	logger.info("file bytes written:" + writeBytes);
                future.channel().close();
            	success = false;
    			reply = future.cause().getMessage();
            }
        }
	};
}
