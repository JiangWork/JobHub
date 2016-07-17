package org.smartframework.jobhub.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.log4j.Logger;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * Handle the received file data.
 * @author Miller
 * @date Jul 2, 2016 2:50:53 PM
 */
public class UploadServerHandler extends ChannelInboundHandlerAdapter {

	private static final Logger logger = Logger.getLogger(UploadServerHandler.class);
	
	private String uploadDirectory = System.getProperty("java.tmp.dir", "/tmp") ;
	
	private ByteBuf buf = Unpooled.buffer(2048) ;
	private int headerLength = -1;
	private boolean headerRead = false;
	private long fileLength = -1;
	private String fileName = "";
	private String outFilePath = "";
	private long byteRead = 0;
	
	private FileOutputStream fos = null;
	
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws IOException, InterruptedException {
    	ByteBuf m = (ByteBuf) msg;
    	buf.writeBytes(m); // (2)
		m.release();
		if (headerLength == -1 && buf.readableBytes() >= 4) {
			headerLength = buf.readInt();
			logger.debug("header length:" + headerLength);
		} 
		if (headerLength != -1 && !headerRead && buf.readableBytes() >= headerLength) {
			long jobId = buf.readLong();
			byte[] bytes = new byte[buf.readInt()];
			buf.readBytes(bytes);
			fileName = new String(bytes);
			fileLength = buf.readLong();
			headerRead = true;
			String destDir = uploadDirectory + File.separator + String.valueOf(jobId);
			File file = new File(destDir);
			if (!file.exists()) {
				file.mkdirs();
			}
			outFilePath = destDir + File.separator + fileName;
			
			fos = new FileOutputStream(outFilePath);
			logger.info("reciving fileName:" + fileName + " filelength:" + fileLength);
		}
		if (headerRead) {
			byteRead += buf.readableBytes();
			buf.readBytes(fos, buf.readableBytes());
			buf.clear();
			if (byteRead >= fileLength) {
				fos.close();
				ByteBuf reply = Unpooled.buffer(1024);
				String message = "upload successfully, located at: " + outFilePath;
				reply.writeInt(message.getBytes().length);
				reply.writeBytes(message.getBytes());
				ctx.writeAndFlush(reply).sync();
				ctx.close();
				logger.info("data recived:" + byteRead + " located at: " + outFilePath);
			}
		}
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        // Close the connection when an exception is raised.
        cause.printStackTrace();
        ctx.close();
    }

	public String getUploadDirectory() {
		return uploadDirectory;
	}

	public void setUploadDirectory(String uploadDirectory) {
		this.uploadDirectory = uploadDirectory;
	}
    
    
}