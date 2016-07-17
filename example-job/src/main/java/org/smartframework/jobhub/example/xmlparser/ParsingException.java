package org.smartframework.jobhub.example.xmlparser;

/**
 * Throw a {@link ParsingException} when the passing file cannot be parsed.
 * 
 * @author jiangzhao
 * @date  May 22, 2016
 * @version V1.0
 */
public class ParsingException extends Exception {

	private static final long serialVersionUID = 1L;

	public ParsingException(String msg) {
		super(msg);
	}
	
	public ParsingException(String msg, Throwable cause) {
		super(msg, cause);
	}
	
	
	
}
