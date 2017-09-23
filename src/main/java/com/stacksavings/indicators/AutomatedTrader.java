package com.stacksavings.indicators;

import com.stacksavings.client.api.PoloniexClientApi;
import com.stacksavings.loaders.CsvTicksLoader;
import com.stacksavings.strategies.BuySellStrategy;
import com.stacksavings.utils.FileManager;
import com.stacksavings.utils.PoloniexTraderClient;
import com.stacksavings.utils.PropertiesUtil;
import eu.verdelhan.ta4j.*;
import eu.verdelhan.ta4j.analysis.criteria.TotalProfitCriterion;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.trading.rules.StopLossRule;
import org.joda.time.DateTime;
import java.io.File;

import java.math.BigDecimal;
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
	private PoloniexTraderClient poloniexTraderClient;
	private PropertiesUtil propertiesUtil;
	private boolean liveTradeMode;
	private int numResultsPerCurrency;

	private Decimal initialSpendAmtPerCurrency;

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
		poloniexTraderClient = PoloniexTraderClient.getInstance();
		numResultsPerCurrency = Integer.parseInt(propertiesUtil.getProps().getProperty("num_results_per_currency"));
	}
	
	
	/**
	 * calculateROC
	 */
	public void run(final String fromDate, final String toDate, final List<String> currencySkipList, final boolean liveTradeMode)
	{
		this.liveTradeMode = liveTradeMode;

		List<String> currencyPairList = poloniexClientApi.returnCurrencyPair();

		//NOTE: This method is being used, at the moment, for initial testing of some concepts, it will need to be refactored to be more
		//organized later on, as this one method should not be handling all of this logic for long-term purposes

		final Decimal startingBTC = Decimal.valueOf(0.025893377);

		//this is only for the first trade for a currency
		initialSpendAmtPerCurrency = startingBTC.dividedBy(Decimal.valueOf(currencyPairList.size()));

		final Map<String, List<Decimal>> currencyTotals = new HashMap<String, List<Decimal>>();

		/** The loss ratio threshold (e.g. 3 for 3%) */
		final Decimal stopLossRatio = Decimal.valueOf(2.5);

		final List<String> currenciesEndingWithLoss = new ArrayList<String>();

		if (liveTradeMode) {
			System.out.println("******* BEGIN live trading iteration *******");
		}

		if(currencyPairList != null && currencyPairList.size() > 0)
		{
			for (String currency : currencyPairList)
			{
				try {
					if (currencySkipList != null && currencySkipList.contains(currency)) {
						continue;
					}

					if (poloniexTraderClient.areOpenPoloniexOrders(currency, null)) {
						//TODO this logic is very rough, we need to figure a lot more on this as there could be orders
						//that stay open a long time so we need logic to potentially cancel the order, etc, for now just skip
						//if it has open poloniex order as we can't do another trade while it's open
						continue;
					}

					String fileNameCurrencyPair = null;
					if (liveTradeMode) {
						fileNameCurrencyPair = fileManager.getFileNameByCurrency(currency);
					} else {
						final File currencyPairFile = fileManager.getFileByName(fromDate, toDate, currency);
						fileNameCurrencyPair = currencyPairFile.getAbsolutePath();
					}

					final TimeSeries series = csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair);

					//TODO We should consider pulling the time frame values from a config file
					final Strategy strategy = BuySellStrategy.buildStrategyEMA(series, 9, 26);

					final ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
					final Rule stopLossRule = new StopLossRule(closePrice, stopLossRatio);


					// Initializing the trading history
					final TradingRecord tradingRecord = new TradingRecord(); //BaseTradingRecord();
					System.out.println("************************************************************");
					System.out.println("Currency: " + currency);

					final Decimal startingFunds = null;

					if (liveTradeMode) {

						synchTradeAccountRecords(tradingRecord, currency);

						//process only the most recent tick as that is the only one that is relevant in real time trading
						processTick(currency, tradingRecord, series, strategy, stopLossRule, startingFunds, series.getEnd());

					} else {
						//backtesting
						for (int i = 0; i < series.getTickCount(); i++) {
							processTick(currency, tradingRecord, series, strategy, stopLossRule, startingFunds, i);

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

							final Decimal totalPercentChange = calculatePercentChange(startingFunds, endingFunds);
							if (totalPercentChange.isNegative()) {
								currenciesEndingWithLoss.add(currency);
							}

							System.out.println("Total % change: " + totalPercentChange);

							currencyTotals.put(currency, Arrays.asList(startingFunds, endingFunds));
						} else {
							System.out.println("No trades made");
						}
					}
				} catch (final Exception e) {
					System.out.println("Exception encountered for currency " + currency + ", stack trace follows: ");
					e.printStackTrace();
				}

			}
			if (!liveTradeMode) {
				calculateOverallGainLoss(currencyTotals, currenciesEndingWithLoss);
			}

		} 
		else
		{
			System.out.println("Date missing, unable to process");
		}

		if (liveTradeMode) {
			System.out.println("******* END live trading iteration *******");
		}

	}

	private void processTick(final String currencyPair, final TradingRecord tradingRecord, final TimeSeries series, final Strategy strategy,
							 final Rule stopLossRule, final Decimal startingFunds, final int curIndex) {

		final Tick tick = series.getTick(curIndex);

		final boolean checkForStopLoss = false;
		if (checkForStopLoss) {
			//If stop loss is triggered, take this out of any future trading
			//This is an experiment to try removing ones that trigger a stop loss, the idea is that we may want to decide not to trade
			//certain currencies at all, as they may be too volatile for an algorithm
			if (tradingRecord != null && !tradingRecord.isClosed()) {

				final Decimal lastEntryPrice = tradingRecord.getLastEntry().getPrice();
				boolean shouldStopLoss = (stopLossRule == null ? false : stopLossRule.isSatisfied(curIndex, tradingRecord));
				if (shouldStopLoss) {
					boolean exited = tradingRecord.exit(curIndex, tick.getClosePrice(), tradingRecord.getLastEntry().getAmount());
					if (exited) {
						Order exit = tradingRecord.getLastExit();
						System.out.println("STOP LOSS TRIGGERED, TRADING HALTED for loss of %: " +
								calculatePercentChange(lastEntryPrice, exit.getPrice()) + " on index: " + exit.getIndex()
								+ " (price=" + exit.getPrice().toDouble()
								+ ", amount=" + exit.getAmount().toDouble() + ")");
						return;
					}
				}
			}
		}


		if (strategy.shouldEnter(curIndex, tradingRecord)) {
			// Our strategy should enter
			System.out.println("Strategy should ENTER on " + curIndex);

			Decimal numberToBuy = determineTradeAmount(tradingRecord, tick.getClosePrice());

			boolean entered = enterTrade(currencyPair, tradingRecord, tick.getClosePrice(), curIndex, numberToBuy);

			if (entered) {
				Order entry = tradingRecord.getLastEntry();
				System.out.println("Entered on " + entry.getIndex()
						+ " (price=" + entry.getPrice().toDouble()
						+ ", amount=" + entry.getAmount().toDouble() + ")");
			}
		} else if (strategy.shouldExit(curIndex, tradingRecord)) {
			// Our strategy should exit
			System.out.println("Strategy should EXIT on " + curIndex);

			if (tradingRecord != null && tradingRecord.getLastEntry() != null) {
				Decimal exitAmount = tradingRecord.getLastEntry().getAmount();

				boolean exited = exitTrade(currencyPair, tradingRecord, tick.getClosePrice(), curIndex, exitAmount);

				if (exited) {
					Order exit = tradingRecord.getLastExit();
					System.out.println("Exited on " + exit.getIndex()
							+ " (price=" + exit.getPrice().toDouble()
							+ ", amount=" + exit.getAmount().toDouble() + ")");
				}
			}
		}

	}

	private boolean enterTrade(final String currencyPair, final TradingRecord tradingRecord, final Decimal closePrice,
							   final int curIndex, final Decimal numberToBuy) {

		boolean entered = false;
		if (!liveTradeMode) {
			entered = tradingRecord.enter(curIndex, closePrice, numberToBuy);
		} else {

			final Decimal buyPriceDecimal = closePrice;

			final BigDecimal buyPrice = BigDecimal.valueOf(buyPriceDecimal.toDouble());

			poloniexTraderClient.buy(currencyPair, buyPrice, BigDecimal.valueOf(numberToBuy.toDouble()));

			//TODO, figure what to do on this, we aren't necessarily going to know right away if a trade actually was processed for real time trading
			entered = tradingRecord.enter(curIndex, closePrice, numberToBuy);

		}

		return entered;
	}

	private boolean exitTrade(final String currencyPair, final TradingRecord tradingRecord, final Decimal closePrice,
							   final int curIndex, final Decimal numberToSell) {

		boolean exited = false;
		if (!liveTradeMode) {
			exited = tradingRecord.exit(curIndex, closePrice, numberToSell);
		} else {

			final Decimal sellPriceDecimal = closePrice;

			final BigDecimal sellPrice = BigDecimal.valueOf(sellPriceDecimal.toDouble());

			poloniexTraderClient.sell(currencyPair, sellPrice, BigDecimal.valueOf(numberToSell.toDouble()));

			//TODO, figure what to do on this, we aren't necessarily going to know right away if a trade actually was processed for real time trading
			exited = tradingRecord.exit(curIndex, closePrice, numberToSell);

		}

		return exited;
	}

	/**
	 * When the program is run in trading mode it has to first check the poloniex orders from before and at least update the most recent one
	 * as a trading record trade so that we know the current status of that currency
	 * @param tradingRecord
	 */
	private void synchTradeAccountRecords(final TradingRecord tradingRecord, final String currency) {

		poloniexTraderClient.createTradingRecordFromPoloniexTrade(currency, tradingRecord);
	}

	private Decimal determineTradeAmount(final TradingRecord tradingRecord, final Decimal currentPrice) {
		boolean isFirstTrade = true;
		if (tradingRecord == null || tradingRecord.getTradeCount() > 0) {
			isFirstTrade = false;
		}

		Decimal availableFunds = Decimal.ZERO;

		if (isFirstTrade) {
			availableFunds = initialSpendAmtPerCurrency;
		} else {
			availableFunds = tradingRecord.getLastExit().getPrice().multipliedBy(tradingRecord.getLastExit().getAmount());
		}

		final Decimal amount = availableFunds.dividedBy(currentPrice);

		return amount;

	}

	private void calculateOverallGainLoss(final Map<String, List<Decimal>> currencyTotals, final List<String> currenciesEndingWithLoss) {

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
		System.out.println("************************************************************");
		System.out.println("Currencies ending with loss:");
		for (final String currency : currenciesEndingWithLoss) {
			System.out.println(currency);
		}

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

		
	}
}
