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

		final boolean downloadData = false;
		final boolean runTrader = true;

		// yyyy-MM-dd HH:mm:ss
		String fromDate = "2017-07-01 00:00:00";
		// yyyy-MM-dd HH:mm:ss
		String toDate = "2017-09-19 00:00:00";

		if (downloadData) {
			FileCleaner.getInstance().clearDirectory();

			PoloniexClientApi.getInstance().execute(fromDate, toDate);

			//PoloniexClientApi.getInstance().execute();
		}


		if (runTrader) {
			//ROCIndicatorCalculate.getInstance().calculateROC();

			AutomatedTrader.getInstance().run(fromDate, toDate);
		}
		
	}
}
