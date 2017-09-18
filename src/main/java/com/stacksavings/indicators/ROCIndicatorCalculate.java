package com.stacksavings.indicators;

import java.util.ArrayList;
import java.util.List;

import com.stacksavings.client.api.PoloniexClientApi;
import com.stacksavings.client.api.dto.ROCIndicatorDto;
import com.stacksavings.loaders.CsvTicksLoader;
import com.stacksavings.strategies.BuySellStrategy;
import com.stacksavings.utils.Constants;
import com.stacksavings.utils.FileManager;
import com.stacksavings.utils.PropertiesUtil;
import com.stacksavings.utils.ROCIndicatorUtils;
import eu.verdelhan.ta4j.analysis.criteria.TotalProfitCriterion;


import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.Trade;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.ROCIndicator;
import org.joda.time.DateTime;
import org.mockito.internal.util.collections.ListUtil;

/**
 * 
 * @author jpcol
 *
 */
public class ROCIndicatorCalculate {

	private static ROCIndicatorCalculate instance;
	private CsvTicksLoader csvTicksLoader;
	private PoloniexClientApi poloniexClientApi;
	private FileManager fileManager;
	private PropertiesUtil propertiesUtil;
	private int numResultsPerCurrency;
	
	/**
	 * 
	 * @return
	 */
	public static ROCIndicatorCalculate getInstance() 
	{
	      if(instance == null) 
	      {
	         instance = new ROCIndicatorCalculate();
	      }
	      
	      return instance;
	}	
	
	/**
	 * 
	 */
	private ROCIndicatorCalculate()
	{
		csvTicksLoader = CsvTicksLoader.getInstance();
		poloniexClientApi = PoloniexClientApi.getInstance();
		fileManager = FileManager.getInstance();
		propertiesUtil = PropertiesUtil.getInstance();
		numResultsPerCurrency = Integer.parseInt(propertiesUtil.getProps().getProperty("num_results_per_currency"));
	}
	
	
	/**
	 * calculateROC
	 */
	public void calculateROC()
	{
		List<String> currencyPairList = poloniexClientApi.returnCurrencyPair();

		//NOTE: This method is being used, at the moment, for initial testing of some concepts, it will need to be refactored to be more
		//organized later on, as this one method should not be handling all of this logic for long-term purposes

		//.5 BTC, which should be more than $1,500 USD, depending, of course, on current BTC price
		final Decimal startingBTC = Decimal.valueOf(.01);
		final Decimal amountSpendPerCurrency = startingBTC.dividedBy(Decimal.valueOf(currencyPairList.size()));


		if(currencyPairList != null && currencyPairList.size() > 0)
		{

			final List<TradingRecord> tradingRecords = new ArrayList<TradingRecord>();


			for (String currency : currencyPairList)
			{
				String fileNameCurrencyPair = fileManager.getFileNameByCurrency(currency);
				
				final TimeSeries series = csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair);



			//	TimeSeriesManager seriesManager = new TimeSeriesManager(series);
			//	TradingRecord tradingRecord = seriesManager.run(strategy);
			//	System.out.println("Number of trades for the strategy: " + tradingRecord.getTradeCount());

				// Analysis


				final ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

				//TODO We should consider pulling the time frame values from a config file
				final Strategy strategy = BuySellStrategy.buildStrategyEMA(series, 9, 26);

				//TODO, This feels very wrong, as it seems we should be able to do this later on, to determine exactly how many to buy, based on price
				// at that time, but, for now this will do for testing purposes.

				Decimal numberToBuy = amountSpendPerCurrency.dividedBy(series.getFirstTick().getOpenPrice());



				final TradingRecord buyTradingRecord =  series.run(strategy, Order.OrderType.BUY, numberToBuy);




				System.out.println();
				System.out.println();
				System.out.println("----------------");
				System.out.println();
				System.out.println("Number to buy: " + numberToBuy);
				System.out.println();

				final List<Trade> trades = buyTradingRecord.getTrades();
				if (trades.size() > 0) {


					final Decimal price = buyTradingRecord.getTrades().get(0).getEntry().getPrice();
					final Decimal amount = buyTradingRecord.getTrades().get(0).getEntry().getAmount();
					final Decimal orderTotal = price.multipliedBy(amount);

					System.out.println();
					System.out.println("Holdings after initial trade: " + orderTotal);
					System.out.println();

					//final double totalProfit = new TotalProfitCriterion().calculate(series, buyTradingRecord);
					//System.out.println("Total profit for the strategy: " + totalProfit);



					for (final Trade trade : buyTradingRecord.getTrades()) {
						printTradeHelper(trade, currency, series);
					}

					final int lastTradeIndex = buyTradingRecord.getTrades().size() - 1;
					final Decimal lastPrice = buyTradingRecord.getTrades().get(lastTradeIndex).getExit().getPrice();
					final Decimal lastAmount = buyTradingRecord.getTrades().get(lastTradeIndex).getExit().getAmount();
					final Decimal lastOrderTotal = lastPrice.multipliedBy(lastAmount);

					//TODO, I think that it is possible that the last entry trade may not have exited, as it could still be holding
					//so in those cases, this number would be misleading, as it would be showing the ext from the trade before that,
					//need to confirm this
					System.out.println();
					System.out.println("Holdings after last trade: " + lastOrderTotal);
					System.out.println();

					System.out.println("----------------");
					System.out.println();
					System.out.println();


					final TradingRecord tradingRecord = series.run(strategy);

					final int mostRecentTradeIndex = trades.size() - 1;
					final Trade mostRecentTrade = trades.get(mostRecentTradeIndex);

					//TODO Not sure if these are needed, need to check more into this
					final boolean runAmountCode = false;
					if (runAmountCode) {
						mostRecentTrade.operate(mostRecentTradeIndex, mostRecentTrade.getEntry().getPrice(), amountSpendPerCurrency);

						//TODO Not sure what this does, it seems the operate method is what is needed, not this, but this could be needed also for something
						tradingRecord.enter(mostRecentTradeIndex, mostRecentTrade.getEntry().getPrice(), amountSpendPerCurrency);
					}

				}
			}

		} 
		else
		{
			System.out.println("No hay datos del servicio web");
		}

	}


	public static void printTradeHelper(final Trade trade, final String currency, final TimeSeries series) {
		System.out.println("**** TRADE for " + currency);

		//TODO Not sure if getEndtime() is the right time that we want
		final DateTime enterTime = series.getTick(trade.getEntry().getIndex()).getEndTime();
		final DateTime exitTime = series.getTick(trade.getExit().getIndex()).getEndTime();

		final Decimal entryPrice = trade.getEntry().getPrice();
		final Decimal exitPrice = trade.getExit().getPrice();

		final Decimal amount = trade.getEntry().getAmount();

		final Decimal percentChange = exitPrice.minus(entryPrice).dividedBy(entryPrice).multipliedBy(Decimal.valueOf(100));

		System.out.println();

		System.out.println("Enter: " + entryPrice + " Time: " + enterTime + " Amount: " + amount + " isNew: " + trade.isNew());
		System.out.println("Exit: " + exitPrice + " Time: " + exitTime);
		System.out.println("Gain %: " + percentChange);

		System.out.println();
	}
	
	public static void main(String[] args) {

		ROCIndicatorCalculate.getInstance().calculateROC();
		
	}
}
