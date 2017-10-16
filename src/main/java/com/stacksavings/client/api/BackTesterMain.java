package com.stacksavings.client.api;

import com.stacksavings.Parameter.Parameters;
import com.stacksavings.allocation.Allocator;
import com.stacksavings.allocation.AllocatorBasic;
import com.stacksavings.controller.AutomatedTrader;
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

	final static List<String> currencyIncludeList = Arrays.asList(
			"BTC_ETH",
			"BTC_XRP",
			"BTC_STR",
			"BTC_BCH",
			"BTC_LTC",
			"BTC_ETH",
			"BTC_DASH",
			"BTC_OMG",
			"BTC_XMR",
			"BTC_ZEC",
			"BTC_BTS",
			"BTC_ETC",
			"BTC_SC",
			"BTC_DGB",
			"BTC_LSK",
			"BTC_XEM",
			"BTC_FCT",
			"BTC_STRAT",
			"BTC_MAID",
			"BTC_ZRX",
			"BTC_VTC",
			"BTC_DOGE",
			"BTC_NXT",
			"BTC_BCN",
			"BTC_GAS",
			"BTC_GTC",
			"BTC_XMR",
			"BTC_OMG",
			"BTC_CVC",
			"BTC_DCR",
			"BTC_SYS",
			"BTC_VIA",
			"BTC_NAV",
			"BTC_ARDR",
			"USDT_BTC"
	);

	//Currencies that have been determined to potentially be less likely to be profitable
	final static List<String> currencySkipList = Arrays.asList(
			"data_feed_long",
			"BTC_GAME",
			"BTC_SYS",
			"BTC_AMP",
			"BTC_LTC",
			"BTC_NXC",
			"BTC_GAS",
			"BTC_SC",
			"BTC_VIA",
			"BTC_XVC",
			"BTC_PINK",
			"BTC_ARDR",
			"BTC_OMG",
			"BTC_MAID",
			"BTC_GNT",
			"BTC_GNO",
			"BTC_STRAT",
			"BTC_LBC",

			//new currencies
			"BTC_BCH",
			"BTC_ZRX",
			"BTC_CVC"
	);

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) 
	{

		final boolean downloadData = true;
		final boolean runBackTest = true;

		//This is only for back testing:
		// yyyy-MM-dd HH:mm:ss

		//String fromDate = "2017-08-01 00:00:00";
		String fromDate = "2017-09-28 00:00:00";
		// yyyy-MM-dd HH:mm:ss
		String toDate = "2017-09-30 00:00:00";

		final List<Parameters> parameters = getParameters(fromDate, toDate);

		if (downloadData) {
			FileCleaner.getInstance().clearDirectory();

			//hack for now, this shoulld be refactored to be more elegant as the stategyholder is also created again for the params
			final StrategyHolder strategyHolder = new EMAStrategyHolder(9, 26);

			PoloniexClientApi.getInstance().execute(fromDate, toDate, CONVERSION_CURRENCY, strategyHolder, currencyIncludeList, currencySkipList);

		}

		if (runBackTest) {

			for (final Parameters params : parameters) {
				final AutomatedTrader trader = new AutomatedTrader(params);
				run(trader, true);
			}
		}

	}

	private static Parameters getDefaultParameters1(final String fromDate, final String toDate, final StrategyHolder strategyHolder) {
		Parameters params = new Parameters();
		params.setLiveTradeMode(false);
		params.setProcessStopLoss(false);
		//if it is 0.97 this would be a 3% stop loss, for example
		params.setStopLossRatio(Decimal.valueOf(0.97));
		params.setApplyExperimentalIndicator(false);
		params.setInitialCurrencyAmount(Decimal.valueOf(100));
		params.setConversionCurrency("usdt_btc");
		params.setFeeAmount(Decimal.valueOf(.0025));
		params.setFromDate(fromDate);
		params.setToDate(toDate);
		params.setUseConversionSeries(true);
		params.setCurrencyIncludeList(currencyIncludeList);
		params.setCurrencySkipList(currencySkipList);
		params.setStrategyHolder(strategyHolder);
		Allocator allocator = new AllocatorBasic(params);
		params.setAllocator(allocator);

		return params;
	}

	private static List<Parameters> getParameters(final String fromDate, final String toDate) {

		final List<Parameters> parameters = new ArrayList<>();


		final StrategyHolder strategyHolder = new EMAStrategyHolder(9, 26);

		Parameters params = getDefaultParameters1(fromDate, toDate, strategyHolder);
		params.setUseCachedBuySellSignals(true);
		parameters.add(params);

		params = getDefaultParameters1(fromDate, toDate, strategyHolder);
		//parameters.add(params);

		params = getDefaultParameters1(fromDate, toDate, strategyHolder);
		params.setCurrencyIncludeList(null);
		//parameters.add(params);

		params = getDefaultParameters1(fromDate, toDate, strategyHolder);
		params.setCurrencyIncludeList(null);
		params.setCurrencySkipList(null);
		//parameters.add(params);


		return parameters;

	}

	private static void run(final AutomatedTrader trader, final boolean runBackTest) {
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
