package org.smartframework.jobhub.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;

import org.apache.log4j.Logger;



public class IOUtils {
	
	private final static Logger logger = Logger.getLogger(IOUtils.class);
	
	public static String read(String filePath) {
		StringBuilder sb = new StringBuilder();
		try {
			File file = new File(filePath);
			BufferedReader br = new BufferedReader(new FileReader(file));
			String inLine = null;
			while((inLine = br.readLine()) != null) {
				sb.append(inLine);
				sb.append('\n');
			}
			br.close();
		} catch(Exception e) {
			logger.error(e.getMessage(), e);
		}
		return sb.toString();
	}
	
	public static void closeQuietly(OutputStream os) {
		if (os == null) {
			return;
		}
		try {
			os.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public static void closeQuietly(InputStream is) {
		if (is == null) {
			return;
		}
		try {
			is.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	public static void closeQuietly(Writer os) {
		if (os == null) {
			return;
		}
		try {
			os.close();
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}

}
