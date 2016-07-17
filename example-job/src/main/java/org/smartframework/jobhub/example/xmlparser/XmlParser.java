package org.smartframework.jobhub.example.xmlparser;


/**
 * 
 * A interface to define the Parser functionalities.
 * 
 * @author jiangzhao
 * @date  May 22, 2016
 * @version V1.0
 * @param <T>
 */
public interface XmlParser <T> {
	
	/**
	 * Given a xmlPath, convert the elements into java object.
	 * 
	 * @param xmlPath the path of XML file.
	 * @return the desired java object.
	 * @throws ParsingException error occur when do the parse.
	 */
	public T parse(String xmlPath) throws ParsingException;
}
