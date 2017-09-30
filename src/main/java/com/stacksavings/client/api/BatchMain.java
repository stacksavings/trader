package com.stacksavings.client.api;

import com.stacksavings.indicators.AutomatedTrader;
import com.stacksavings.utils.FileCleaner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author jpcol
 *
 */
public class BatchMain {

	private final static String CONVERSION_CURRENCY = "usdt_btc";

	private static BatchMain instance;
	
	public static BatchMain getInstance() 
	{
	      if(instance == null) 
	      {
	         instance = new BatchMain();
	      }
	      
	      return instance;
	}	

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) 
	{

		//Currencies that have been determined to potentially be less likely to be profitable
		List<String> currencySkipList = Arrays.asList(
				"BTC_BELA",
				"BTC_XBC",
				"BTC_GAME",
				"BTC_GAS",
				"BTC_OMNI",
				"BTC_NXT",
				"BTC_SC",
				"BTC_RIC",
				"BTC_STEEM",
				"BTC_ZRX",
				"BTC_FCT",
				"BTC_CVC",
				"BTC_ETC",
				"BTC_LBC"
		);

		//skip this for now as it may not be needed
		currencySkipList = new ArrayList();

		final boolean downloadData = false;
		final boolean runTrader = true;
		final boolean liveTradeMode = false;

		//This is only for back testing:
		// yyyy-MM-dd HH:mm:ss
		String fromDate = "2017-05-01 00:00:00";
		// yyyy-MM-dd HH:mm:ss
		String toDate = "2017-09-29 00:00:00";

		if (!liveTradeMode && downloadData) {
			FileCleaner.getInstance().clearDirectory();

			PoloniexClientApi.getInstance().execute(fromDate, toDate, CONVERSION_CURRENCY);

		} else if( liveTradeMode) {
			PoloniexClientApi.getInstance().execute(CONVERSION_CURRENCY);
		}

		try {
			if (!liveTradeMode && runTrader) {

				AutomatedTrader.getInstance().run(fromDate, toDate, currencySkipList, liveTradeMode);
			} else if (liveTradeMode) {
				AutomatedTrader.getInstance().run(null, null, currencySkipList, liveTradeMode);
			}
		} catch (final  Exception e) {
			System.out.println("Exception encountered");
			e.printStackTrace();
		}


		
	}
}
