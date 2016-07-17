package org.smartframework.jobhub.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

/**
 * A convenient way to access different type of parameters.
 * 
 * @author jiangzhao
 * @date Jun 16, 2016
 * @version V1.0
 */
public class ParameterAccessor {
	
	private Map<String, String> parameters;
	
	public ParameterAccessor(Map<String, String> parameters) {
		this.parameters = parameters;
	}
	
	public ParameterAccessor(Properties prop) {
		parameters = new HashMap<String, String>();
		Iterator<Entry<Object, Object>> iter = prop.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<Object, Object> entry = iter.next();
			parameters.put((String)entry.getKey(), (String)entry.getValue());
		}
		
	}
	
	public boolean getBoolean(String key, boolean defaultVal) {
		boolean retVal = defaultVal;
		if (parameters.containsKey(key)) {
			retVal = Boolean.parseBoolean(parameters.get(key));
		}
		return retVal;
	}
	
	public short getShort(String key, short defaultVal) {
		short retVal = defaultVal;
		if (parameters.containsKey(key)) {
			String value = parameters.get(key);
			try {
				retVal = Short.parseShort(value);
			} catch(Exception e) {
				// do nothing
			}
		} 
		return retVal;	
	}
	
	public int getInt(String key, int defaultVal) {
		int retVal = defaultVal;
		if (parameters.containsKey(key)) {
			String value = parameters.get(key);
			try {
				retVal = Integer.parseInt(value);
			} catch(Exception e) {
				// do nothing
			}
		} 
		return retVal;
	}
	
	public String getString(String key, String defaultVal) {
		String retVal = defaultVal;
		if (parameters.containsKey(key)) {
			retVal = parameters.get(key);
		}
		return retVal;
	}
	
	public long getLong(String key, long defaultVal) {
		long retVal = defaultVal;
		if (parameters.containsKey(key)) {
			try {
				retVal = Long.parseLong(parameters.get(key));
			} catch(Exception e) {
				// do nothing
			}
		}
		return retVal;
	}

	public double getDouble(String key, double defaultVal) {
		double retVal = defaultVal;
		if (parameters.containsKey(key)) {
			try {
				retVal = Double.parseDouble(parameters.get(key));
			} catch(Exception e) {
				// do nothing
			}
		}
		return retVal;
	}
	
	/**
	 * Get a list from {@code parameter}. If no such key exists, 
	 * an empty list will be returned, else split the value using 
	 * {@code delimiter}.
	 * @param key the key to get list.
	 * @param delimiter the delimiter to split the value.
	 * @return a arraylist.
	 */
	public List<String> getList(String key, String delimiter) {
		List<String> ret = new ArrayList<String>();
		if (parameters.containsKey(key)) {
			String value = parameters.get(key);
			String[] fields = value.split(delimiter);
			ret = Arrays.asList(fields);
		}
		return ret;		
	}
	
	/**
	 * Get a map from {@code parameter}. If no such key exists, 
	 * an empty map will be returned, else parse the value using
	 * {@code interDel} to get key-value pairs, then use {@code intraDel}
	 * to get key and value.
	 * @param key the key to get map.
	 * @param interDel the key-value pair delimiter
	 * @param intraDel the key and value delimiter.
	 * @return a hashmap.
	 */
	public Map<String, String> getMap(String key, String interDel, String intraDel) {
		Map<String, String> map = new HashMap<String, String>();
		if(parameters.containsKey(key)) {
			String value = parameters.get(key);
			String[] keyValuePairs = value.split(interDel);
			for (String keyValue: keyValuePairs) {
				int index =  keyValue.indexOf(intraDel);
				if (index == -1) {
					map.put(keyValue, "");
				} else {
					map.put(keyValue.substring(0, index), keyValue.substring(index + 1));
				}
			}
		}
		return map;
	}
	
	/**
	 * Get boolean value from {@code parameter},
	 * false is returned if key does not exist in {@code parameter}.
	 * @param key
	 * @return
	 */
	public boolean getBoolean(String key) {
		return this.getBoolean(key, false);
	}
	
	/**
	 * Get short value from {@code parameter}.
	 * 0 is returned if key does not exist in {@code parameter}.
	 * @param key
	 * @return
	 */
	public short getShort(String key) {
		return this.getShort(key, (short)0);
	}
	
	/**
	 * Get int value from {@code parameter},
	 * 0 is returned if key does not exist in {@code parameter}.
	 * 
	 * @param key
	 * @return
	 */
	public int getInt(String key) {
		return this.getInt(key, 0);
	}
	
	/**
	 * Get string value from {@code parameter},
	 * "" is return if key does not exist in {@code parameter}.
	 * 
	 * @param key
	 * @return
	 */
	public String getString(String key) {
		return this.getString(key, "");
	}
	

	/**
	 * Get double value from {@code parameter},
	 * 0 is return if key does not exist in {@code parameter}.
	 * 
	 * @param key
	 * @return

	 */
	public double getDouble(String key) {
		return this.getDouble(key, 0);
	}
	
	
	/**
	 * Get a list from {@code parameter}. Use , to split the value.
	 * @param key
	 * @return a arraylist.
	 * @see ParameterAccessor#getList(String, String)
	 */
	public List<String> getList(String key) {
		return this.getList(key, ",");
	}
	
	/**
	 * Get a map from {@code parameter}. Use , and : to split the value.
	 * @param key
	 * @return a hashmap.
	 * @see ParameterAccessor#getMap(String, String, String)
	 */
	public Map<String, String> getMap(String key) {
		return this.getMap(key, ",", ":");
	}
	
	public String toString() {
		return this.parameters.toString();
	}
}
