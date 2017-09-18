package com.stacksavings.client.api;

import com.stacksavings.indicators.AutomatedTrader;
import com.stacksavings.indicators.ROCIndicatorCalculate;
import com.stacksavings.utils.FileCleaner;

/**
 * 
 * @author jpcol
 *
 */
public class BatchMain {

	private static BatchMain instance;
	
	public static BatchMain getInstance() 
	{
	      if(instance == null) 
	      {
	         instance = new BatchMain();
	      }
	      
	      return instance;
	}	
	
	public void execute()
	{
		FileCleaner.getInstance().clearDirectory();
		
		PoloniexClientApi.getInstance().execute();
		
		ROCIndicatorCalculate.getInstance().calculateROC();
	}
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) 
	{

		FileCleaner.getInstance().clearDirectory();
		
		PoloniexClientApi.getInstance().execute();

		//ROCIndicatorCalculate.getInstance().calculateROC();

		AutomatedTrader.getInstance().run();
		
	}
}
