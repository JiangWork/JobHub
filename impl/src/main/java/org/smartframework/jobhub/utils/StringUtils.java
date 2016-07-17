package org.smartframework.jobhub.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class StringUtils {
	
	public final static String EMPTY = "";
	
	public static String stringfyObject(Object obj) {
		if (obj == null) {
			return EMPTY;
		}
		return String.valueOf(obj);
	}
	
	/**
	 * Stringfy the list, use , as delimiter.
	 * @param list
	 * @return
	 */
	public static String stringfyList(List<?> list) {
		if (list == null) {
			return EMPTY;
		}
		StringBuilder sb = new StringBuilder();
		for(Object obj: list) {
			sb.append("," + String.valueOf(obj));
		}
		return sb.length() >= 1 ? sb.toString().substring(1) : "";
	}
	
	public static String stringfyMap(Map<? extends Object, ? extends Object> map) {
		if (map == null) {
			return EMPTY;
		}
		StringBuilder sb = new StringBuilder();
		Iterator<?> iter = map.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<Object, Object> ent = (Entry<Object, Object>) iter.next();
			sb.append(",");
			sb.append(String.valueOf(ent.getKey()) + ":" + String.valueOf(ent.getValue()));
		}
		return sb.length() >= 1 ? sb.toString().substring(1) : "";
	}
	
	public static String stringfyException(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
}
