package com.stacksavings.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 
 * @author jpcol
 *
 */
public class PropertiesUtil {

	private static PropertiesUtil instance;
	private Properties props;
	
	public static PropertiesUtil getInstance() {
		if (instance == null) {
			instance = new PropertiesUtil();
		}

		return instance;
	}
	
	private PropertiesUtil(){
		props = new Properties();
		try 
		{
			InputStream resourceStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.properties");
		    props.load(resourceStream);
		} catch (IOException e) 
		{
			e.printStackTrace();
		}
	}
	
	public Properties getProps(){
		return this.props;
	}
}
