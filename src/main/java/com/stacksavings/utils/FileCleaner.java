package com.stacksavings.utils;

/**
 * 
 * @author jpcol
 *
 */
public class FileCleaner {

	private static FileCleaner instance;
	
	private FileManager fileManager;
	
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
	}
	
	public void clearDirectory()
	{
		fileManager.clearDirectory();
	}
	
	public static void main(String[] args) 
	{

		FileCleaner	.getInstance().clearDirectory();
		
	}
}
