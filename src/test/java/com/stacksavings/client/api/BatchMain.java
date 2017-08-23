package com.stacksavings.client.api;

import com.stacksavings.indicators.ROCIndicatorCalculate;
import com.stacksavings.utils.FileCleaner;

/**
 * 
 * @author jpcol
 *
 */
public class BatchMain {

	
	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) 
	{

		FileCleaner.getInstance().clearDirectory();
		
		PoloniexClientApi.getInstance().execute();
		
		ROCIndicatorCalculate.getInstance().calculateROC();
		
	}
}
