package com.stacksavings.client.api;

import com.stacksavings.Parameter.Parameters;
import com.stacksavings.allocation.Allocator;
import com.stacksavings.allocation.AllocatorBasic;
import com.stacksavings.indicators.AutomatedTrader;
import com.stacksavings.strategies.EMAStrategyHolder;
import com.stacksavings.strategies.StrategyHolder;
import com.stacksavings.utils.FileCleaner;
import eu.verdelhan.ta4j.Decimal;
import org.omg.Dynamic.Parameter;

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
			"BTC_LBC"
	);

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) 
	{

		final boolean downloadData = false;
		final boolean runBackTest = true;

		//This is only for back testing:
		// yyyy-MM-dd HH:mm:ss

		//String fromDate = "2017-08-01 00:00:00";
		String fromDate = "2017-05-01 00:00:00";
		// yyyy-MM-dd HH:mm:ss
		String toDate = "2017-09-29 00:00:00";


		if (downloadData) {
			FileCleaner.getInstance().clearDirectory();

			PoloniexClientApi.getInstance().execute(fromDate, toDate, CONVERSION_CURRENCY);

		}

		if (runBackTest) {
			final List<Parameters> parameters = getParameters(fromDate, toDate);
			for (final Parameters params : parameters) {
				final AutomatedTrader trader = new AutomatedTrader(params);
				run(trader, true);
			}
		}

	}

	private static List<Parameters> getParameters(final String fromDate, final String toDate) {

		final List<Parameters> parameters = new ArrayList<>();

		//run 1
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
		params.setCurrencySkipList(currencySkipList);

		final StrategyHolder strategyHolder = new EMAStrategyHolder(9, 26);
		params.setStrategyHolder(strategyHolder);

		final Allocator allocator = new AllocatorBasic(params);
		params.setAllocator(allocator);


		//run 2
		Parameters params2 = new Parameters();
		params2.setLiveTradeMode(false);
		params2.setProcessStopLoss(false);
		params2.setApplyExperimentalIndicator(true);
		params2.setInitialCurrencyAmount(Decimal.valueOf(100));
		params2.setConversionCurrency("usdt_btc");
		params2.setFeeAmount(Decimal.valueOf(.0025));
		params2.setFromDate(fromDate);
		params2.setToDate(toDate);
		params2.setProcessStopLoss(false);
		params2.setUseConversionSeries(true);
		params2.setCurrencySkipList(currencySkipList);

		final StrategyHolder strategyHolder2 = new EMAStrategyHolder(9, 26);
		params2.setStrategyHolder(strategyHolder2);

		final Allocator allocator2 = new AllocatorBasic(params2);
		params2.setAllocator(allocator2);

		parameters.add(params);
		parameters.add(params2);

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
