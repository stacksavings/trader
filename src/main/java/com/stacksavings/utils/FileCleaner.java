package com.stacksavings.utils;

/**
 * 
 * @author jpcol
 *
 */
public class FileCleaner {

	private static FileCleaner instance;
	
	private FileManager fileManager;
	
	private LoggerManager loggerManager;
	
	public static FileCleaner getInstance() 
	{
	      if(instance == null) 
	      {
	         instance = new FileCleaner();
	      }
	      
	      return instance;
	}	
	
	private FileCleaner	()
	{
		fileManager = FileManager.getInstance();
		
		loggerManager = LoggerManager.getInstance();
	}
	
	public void clearDirectory()
	{
		loggerManager.info("begin clearDirectory");
		
		fileManager.clearDirectory();
		
		loggerManager.info("end clearDirectory");
	}
	
	public static void main(String[] args) 
	{

		FileCleaner	.getInstance().clearDirectory();
		
	}
}
