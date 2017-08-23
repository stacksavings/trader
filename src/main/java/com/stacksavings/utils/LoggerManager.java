package com.stacksavings.utils;


/**
 * 
 * @author jpcol
 *
 */
public class LoggerManager {

	private static LoggerManager instance;
	
	public static LoggerManager getInstance() 
	{
	      if(instance == null) 
	      {
	         instance = new LoggerManager();
	      }
	      
	      return instance;
	}	
	
	private LoggerManager()
	{
		
	}
	
	public void info(String message){
		System.out.println("*** INFO *** "+message);
	}

	public void debug(String message){
		System.out.println("*** DEBUG *** "+message);
	}

	public void warning(String message){
		System.out.println("*** ERROR *** "+message);
	}

	public void error(String message){
		System.out.println("*** ERROR *** "+message);
	}

}
