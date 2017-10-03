package com.stacksavings.client.api;

import com.stacksavings.Parameter.Parameters;
import com.stacksavings.indicators.AutomatedTrader;
import com.stacksavings.strategies.EMAStrategyHolder;
import com.stacksavings.strategies.StrategyHolder;
import com.stacksavings.utils.FileCleaner;
import eu.verdelhan.ta4j.Decimal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * @author jpcol
 *
 */
public class BackTesterMain {

	private final static String CONVERSION_CURRENCY = "usdt_btc";

	private static BackTesterMain instance;
	
	public static BackTesterMain getInstance()
	{
	      if(instance == null) 
	      {
	         instance = new BackTesterMain();
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


		final boolean downloadData = false;
		final boolean runBackTest = true;

		//This is only for back testing:
		// yyyy-MM-dd HH:mm:ss

		//String fromDate = "2017-08-01 00:00:00";
		String fromDate = "2017-05-01 00:00:00";
		// yyyy-MM-dd HH:mm:ss
		String toDate = "2017-09-29 00:00:00";


		Parameters params = new Parameters();
		params.setLiveTradeMode(false);
		params.setProcessStopLoss(false);
		params.setApplyExperimentalIndicator(false);
		params.setInitialCurrencyAmount(Decimal.valueOf(100));
		params.setConversionCurrency("usdt_btc");
		params.setFeeAmount(Decimal.valueOf(.0025));
		params.setFromDate(fromDate);
		params.setToDate(toDate);
		params.setProcessStopLoss(false);
		params.setUseConversionSeries(true);

		final StrategyHolder strategyHolder = new EMAStrategyHolder(9, 26);
		params.setStrategyHolder(strategyHolder);


		if (downloadData) {
			FileCleaner.getInstance().clearDirectory();

			PoloniexClientApi.getInstance().execute(fromDate, toDate, CONVERSION_CURRENCY);

		}

		final AutomatedTrader trader = new AutomatedTrader(params);

		try {
			if (runBackTest) {

				trader.run();
			}
		} catch (final  Exception e) {
			System.out.println("Exception encountered");
			e.printStackTrace();
		}


		
	}
}
