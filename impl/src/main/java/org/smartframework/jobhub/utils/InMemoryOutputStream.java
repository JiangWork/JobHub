package org.smartframework.jobhub.utils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.apache.log4j.Logger;

/**
 * 
 * A InMemoryOutputStream which keeps the bytes written in memory, i.e., in a 
 * backing byte array. It is same as {@link ByteArrayOutputStream}, which provides
 * the functionality to write the data in clip mode.
 *
 * @author jiangzhao
 * @date Apr 26, 2016
 * @version V1.0
 */
public class InMemoryOutputStream extends OutputStream {
	
	private static final Logger logger = Logger.getLogger(InMemoryOutputStream.class);
	
	private final static int DEFAULT_MAX_BYTES = 4 * 1024;

    /** 
     * The buffer where data is stored. 
     */
    protected byte buf[];

    /**
     * The number of valid bytes in the buffer. 
     */
    protected int count;
    
    /**
     * if enableClip, exceeded bytes will be discarded to ensure the fixed byte array.
     */
	private boolean enableClip = false;
	
	/**
	 * The max size of the bytes are permitted to be written.
	 */
	private final int maxOutputSize;
	
	/**
	 * The number of clip happens.
	 */
	private int clipNum = 0;

		
	public InMemoryOutputStream() {
		this(false, DEFAULT_MAX_BYTES, 32);
	}
	
	public InMemoryOutputStream(int size) {
		this(false, DEFAULT_MAX_BYTES, size);
	}
	
	public InMemoryOutputStream(boolean enableClip, int maxOutputBytes) {
		this(enableClip, maxOutputBytes, 32);
	}
	
	public InMemoryOutputStream(boolean enableClip, int maxOutputBytes, int size) {
		this.enableClip = enableClip;
		
		if (maxOutputBytes < 32) {
			throw new IllegalArgumentException("Negative or too small size: "
                    + maxOutputBytes);
		}
		this.maxOutputSize = maxOutputBytes;
        if (size < 0) {
            throw new IllegalArgumentException("Negative initial size: "
                                               + size);
        }
	    buf = new byte[size];	
	}
	
    /**
     * Writes <code>len</code> bytes from the specified byte array 
     * starting at offset <code>off</code> to this byte array output stream.
     *
     * @param   b     the data.
     * @param   off   the start offset in the data.
     * @param   len   the number of bytes to write.
     */
    public synchronized void write(byte b[], int off, int len) {
    	if ((off < 0) || (off > b.length) || (len < 0) ||
    			((off + len) > b.length) || ((off + len) < 0)) {
    		throw new IndexOutOfBoundsException();
    	} else if (len == 0) {
    		return;
    	}
    	if (enableClip) {
    		doClipWrite(b, off, len);
    	} else {
    		writeDirectly(b, off, len);
    	}
    	
    }
    
    public synchronized void write(byte b[]) {
    	this.write(b, 0, b.length);
    }

    private void writeDirectly(byte b[], int off, int len) {
    	int newcount = count + len;
    	if (newcount > buf.length) {
    		buf = Arrays.copyOf(buf, Math.max(buf.length << 1, newcount));
    	}
    	System.arraycopy(b, off, buf, count, len);
    	count = newcount;
    }
    
    public synchronized void reset() {
    	count = 0;
    }

    public synchronized byte toByteArray()[] {
        return Arrays.copyOf(buf, count);
    }
    

    public synchronized int size() {
    	return count;
    }


    public synchronized String toString() {
    	return new String(buf, 0, count);
    }
    
    public void close() {
    }

	
	/**
     * Writes <code>len</code> bytes from the specified byte array 
     * starting at offset <code>off</code> to this byte array output stream in clip mode.
     * 
     * In clip mode, the max size of underneath byte array is fixed. 
     * If overflow happens, the oldest data will be omitted.
     * The byte array always keep the latest output. 
     *
     * This method assume the parameters are all correct.
     * 
     * @param   b     the data.
     * @param   off   the start offset in the data.
     * @param   len   the number of bytes to write.
     */
	private void doClipWrite(byte b[], int off, int len) {
		if (count + len <= maxOutputSize) {  // won't overflow
			this.writeDirectly(b, off, len);
		} else {
			int discardBytes = len + count - maxOutputSize;
			if (len >= maxOutputSize) {
				reset();
				this.writeDirectly(b, off + len - maxOutputSize, maxOutputSize);
			} else {
				int shiftStart = count - maxOutputSize + len;
				int remaining = maxOutputSize - len;
				System.arraycopy(buf, shiftStart, buf, 0, remaining);
				count = remaining;
				this.writeDirectly(b, off, len);
			}
			clipNum ++;
			logger.info(String.format("Overflow count: %d, discarded bytes number: %d", clipNum,
					discardBytes));
			
		}

	}

	/**
	 * Not supported yet.
	 */
	public void write(int b) throws IOException {		
	}
	
}
