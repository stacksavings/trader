package com.stacksavings.indicators;

import com.stacksavings.client.api.PoloniexClientApi;
import com.stacksavings.loaders.CsvTicksLoader;
import com.stacksavings.strategies.BuySellStrategy;
import com.stacksavings.utils.FileManager;
import com.stacksavings.utils.PropertiesUtil;
import eu.verdelhan.ta4j.*;
import eu.verdelhan.ta4j.analysis.criteria.TotalProfitCriterion;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.trading.rules.StopLossRule;
import org.joda.time.DateTime;

import java.util.*;

/**
 * TODO this class is in the wrong place, need to figure where this should go, it is not an indicator, just put in this package to try to get it working quickly
 * @author Rickd
 *
 */
public class AutomatedTrader {

	private static AutomatedTrader instance;
	private CsvTicksLoader csvTicksLoader;
	private PoloniexClientApi poloniexClientApi;
	private FileManager fileManager;
	private PropertiesUtil propertiesUtil;
	private int numResultsPerCurrency;

	/**
	 *
	 * @return
	 */
	public static AutomatedTrader getInstance()
	{
	      if(instance == null)
	      {
	         instance = new AutomatedTrader();
	      }

	      return instance;
	}

	/**
	 *
	 */
	private AutomatedTrader()
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
	public void run()
	{
		List<String> currencyPairList = poloniexClientApi.returnCurrencyPair();

		//NOTE: This method is being used, at the moment, for initial testing of some concepts, it will need to be refactored to be more
		//organized later on, as this one method should not be handling all of this logic for long-term purposes

		//.5 BTC, which should be more than $1,500 USD, depending, of course, on current BTC price
		final Decimal startingBTC = Decimal.valueOf(.01);
		final Decimal amountSpendPerCurrency = startingBTC.dividedBy(Decimal.valueOf(currencyPairList.size()));

		final Map<String, List<Decimal>> currencyTotals = new HashMap<String, List<Decimal>>();

		/** The loss ratio threshold (e.g. 3 for 3%) */
		final Decimal stopLossRatio = Decimal.valueOf(2.5);

		if(currencyPairList != null && currencyPairList.size() > 0)
		{

			final List<TradingRecord> tradingRecords = new ArrayList<TradingRecord>();

			//TODO this can get a bit complicated because we will hold positions between runs of this program, for now, it will work for back testing only

			for (String currency : currencyPairList)
			{
				String fileNameCurrencyPair = fileManager.getFileNameByCurrency(currency);
				
				final TimeSeries series = csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair);

				//TODO We should consider pulling the time frame values from a config file
				final Strategy strategy = BuySellStrategy.buildStrategyEMA(series, 9, 26);

				final ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
				final Rule stopLossRule = new StopLossRule(closePrice, stopLossRatio);


				// Initializing the trading history
				TradingRecord tradingRecord = new TradingRecord(); //BaseTradingRecord();
				System.out.println("************************************************************");
				System.out.println("Currency: " + currency);

				Decimal startingFunds = null;

				boolean isFirstTrade = true;

				for (int i = 0; i < series.getTickCount(); i++) {

					final int curIndex = i;

					final Tick tick = series.getTick(i);


					//If stop loss is triggered, take this out of any future trading
					//This is an experiment to try removing ones that trigger a stop loss, the idea is that we may want to decide not to trade
					//certain currencies at all, as they may be too volatile for an algorithm
					if (tradingRecord != null && !tradingRecord.isClosed()) {

						final Decimal lastEntryPrice = tradingRecord.getLastEntry().getPrice();
						boolean shouldStopLoss = stopLossRule.isSatisfied(curIndex, tradingRecord);
						if (shouldStopLoss) {
							boolean exited = tradingRecord.exit(curIndex, tick.getClosePrice(), tradingRecord.getLastEntry().getAmount());
							if (exited) {
								Order exit = tradingRecord.getLastExit();
								System.out.println("STOP LOSS TRIGGERED, TRADING HALTED for loss of %: " +
										calculatePercentChange(lastEntryPrice, exit.getPrice()) + " on index: " + exit.getIndex()
										+ " (price=" + exit.getPrice().toDouble()
										+ ", amount=" + exit.getAmount().toDouble() + ")");
								break;
							}
						}
					}




					if (strategy.shouldEnter(curIndex, tradingRecord)) {
						// Our strategy should enter
						System.out.println("Strategy should ENTER on " + curIndex);

						//TODO rework this into it's own method
						Decimal numberToBuy = Decimal.ONE;
						if (isFirstTrade) {
							//we have to calculate how much this currency is allocated initially to start with
							numberToBuy = amountSpendPerCurrency.dividedBy(tick.getClosePrice());

							startingFunds = numberToBuy.multipliedBy(tick.getClosePrice());
						} else if (tradingRecord.getLastExit() != null) {
							final Decimal availableFunds = tradingRecord.getLastExit().getPrice().multipliedBy(tradingRecord.getLastExit().getAmount());
							numberToBuy = availableFunds.dividedBy(tick.getClosePrice());
						}


						boolean entered = tradingRecord.enter(curIndex, tick.getClosePrice(), numberToBuy);
						if (entered) {
							isFirstTrade = false;
							Order entry = tradingRecord.getLastEntry();
							System.out.println("Entered on " + entry.getIndex()
									+ " (price=" + entry.getPrice().toDouble()
									+ ", amount=" + entry.getAmount().toDouble() + ")");
						}
					} else if (strategy.shouldExit(curIndex, tradingRecord)) {
						// Our strategy should exit
						System.out.println("Strategy should EXIT on " + curIndex);

						Decimal exitAmount =  Decimal.ONE;
						if (!isFirstTrade) {
							exitAmount = tradingRecord.getLastEntry().getAmount();
						}


						boolean exited = tradingRecord.exit(curIndex, tick.getClosePrice(), exitAmount);
						if (exited) {
							Order exit = tradingRecord.getLastExit();
							System.out.println("Exited on " + exit.getIndex()
									+ " (price=" + exit.getPrice().toDouble()
									+ ", amount=" + exit.getAmount().toDouble() + ")");
						}
					}
				}

				if (startingFunds != null) {
					Decimal endingFunds = startingFunds;
					if (tradingRecord.getLastExit() != null) {
						endingFunds = tradingRecord.getLastExit().getPrice().multipliedBy(tradingRecord.getLastExit().getAmount());
					}

					final double totalProfit = new TotalProfitCriterion().calculate(series, tradingRecord);
					System.out.println("Total profit for the strategy: " + totalProfit);
					System.out.println("Total starting funds: " + startingFunds);
					System.out.println("Total ending funds: " + endingFunds);
					System.out.println("Total % change: " + calculatePercentChange(startingFunds, endingFunds));

					currencyTotals.put(currency, Arrays.asList(startingFunds, endingFunds));
				} else {
					System.out.println("No trades made");
				}


			}

			calculateOverallGainLoss(currencyTotals);

		} 
		else
		{
			System.out.println("No hay datos del servicio web");
		}

	}

	private void calculateOverallGainLoss(final Map<String, List<Decimal>> currencyTotals) {

		Decimal start = Decimal.ZERO;
		Decimal end = Decimal.ZERO;

		for (final String currency : currencyTotals.keySet()){

			final Decimal currencyStart = currencyTotals.get(currency).get(0);
			final Decimal currencyEnd = currencyTotals.get(currency).get(1);

			start = start.plus(currencyStart);
			end = end.plus(currencyEnd);

		}

		System.out.println();
		System.out.println();
		System.out.println("************************************************************");
		System.out.println();
		System.out.println("Total start: " + start);
		System.out.println("Total end: " + end);
		System.out.println("Total % change: " + calculatePercentChange(start, end));

	}

	private Decimal calculatePercentChange(final Decimal startingFunds, final Decimal endingFunds) {
		final Decimal percentChange = endingFunds.minus(startingFunds).dividedBy(startingFunds).multipliedBy(Decimal.valueOf(100));
		return percentChange;

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

		AutomatedTrader.getInstance().run();
		
	}
}
