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
import org.jfree.util.StringUtils;
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
	private TimeSeries conversionTimeSeries;

	private final static String CONVERSION_CURRENCY = "btc_usdt";

	final boolean checkForStopLoss = false;

	/** The loss ratio threshold (e.g. 3 for 3%) */
	final Decimal stopLossRatio = Decimal.valueOf(2.5);

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
	public void run(final String fromDate, final String toDate, final List<String> currencySkipList, final boolean liveTradeMode) throws Exception {
		this.liveTradeMode = liveTradeMode;

		final List<String> currencyPairList = poloniexClientApi.returnCurrencyPair();

		//NOTE: This method is being used, at the moment, for initial testing of some concepts, it will need to be refactored to be more
		//organized later on, as this one method should not be handling all of this logic for long-term purposes

		final Decimal startingBTC = Decimal.valueOf(0.025893377);

		//this is only for the first trade for a currency
		initialSpendAmtPerCurrency = startingBTC.dividedBy(Decimal.valueOf(currencyPairList.size()));

		final Map<String, List<Decimal>> currencyTotals = new HashMap<String, List<Decimal>>();

		final List<String> currenciesEndingWithLoss = new ArrayList<String>();

		if (liveTradeMode) {
			System.out.println("******* BEGIN live trading iteration *******");
		}

		if(currencyPairList != null && currencyPairList.size() > 0)
		{

			conversionTimeSeries = getConversionCurrencySeries(currencyPairList, fromDate, toDate);
			if (conversionTimeSeries == null) {
				throw new Exception("Conversion time series came back as null for " + CONVERSION_CURRENCY + ", cannot proceed without this");
			}

			for (String currency : currencyPairList)
			{
				if (skipCurrencyNonTradingReason(currency) || (currencySkipList != null && currencySkipList.contains(currency))) {
					continue;
				}

				try {

					if (poloniexTraderClient.areOpenPoloniexOrders(currency, null)) {
						//TODO this logic is very rough, need to figure a lot more on this as there could be orders
						//that stay open a long time so we need logic to potentially cancel the order, etc, for now just skip
						//if it has open poloniex order as we can't do another trade while it's open
						continue;
					}

					final TimeSeries series = loadTimeSeries(currency, fromDate, toDate);

					//TODO Consider pulling the time frame values from a config file
					final Strategy strategy = BuySellStrategy.buildStrategyEMA(series, 9, 26);

					final ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
					final Rule stopLossRule = new StopLossRule(closePrice, stopLossRatio);

					// Initializing the trading history
					final TradingRecord tradingRecord = new TradingRecord(); //BaseTradingRecord();
					System.out.println("************************************************************");
					System.out.println("Currency: " + currency);

					final Decimal startingFunds = null;

					if (liveTradeMode) {
						runLiveTrade(tradingRecord, currency, series, strategy, stopLossRule);
					} else {
						runBacktest(series, currency, strategy, tradingRecord, startingFunds, currencyTotals, currenciesEndingWithLoss, stopLossRule);
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
		else {
			System.out.println("Date missing, unable to process");
		} if (liveTradeMode) {
			System.out.println("******* END live trading iteration *******");
		}

	}

	private void runLiveTrade(final  TradingRecord tradingRecord, final String currency, final TimeSeries series, final Strategy strategy, final Rule stopLossRule) {
		synchTradeAccountRecords(tradingRecord, currency);

		//process only the most recent tick as that is the only one that is relevant in real time trading
		processTick(currency, tradingRecord, series, strategy, stopLossRule, series.getEnd());
	}

	private void runBacktest(final TimeSeries series, final String currency, final Strategy strategy, final  TradingRecord tradingRecord, final Decimal startingFunds,
							 final Map<String, List<Decimal>> currencyTotals, final List<String> currenciesEndingWithLoss, final Rule stopLossRule) {
		for (int i = 0; i < series.getTickCount(); i++) {
			processTick(currency, tradingRecord, series, strategy, stopLossRule, i);
		}

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

	}

	/**
	 * Skip a currency for a reason other than that it is determined to not be optimal to trade. An example is that we are basing buy / sell off of usdt so we can't also trade it. This differs
	 * from currencies that we skip due to having determined that they may not be optimal to trade from backtesting, for example.
	 * @param currency
	 * @return
	 */
	private boolean skipCurrencyNonTradingReason(final String currency) {
		if (StringUtils.startsWithIgnoreCase(currency, CONVERSION_CURRENCY)) {
			return true;
		}
		return false;
	}

	/**
	 * Load a time series from a currency, optionally a fromDate and toDate
	 * @param currency
	 * @param fromDate
	 * @param toDate
	 * @return
	 */
	private TimeSeries loadTimeSeries(final String currency, final String fromDate, final String toDate) {
		String fileNameCurrencyPair = null;
		if (liveTradeMode) {
			fileNameCurrencyPair = fileManager.getFileNameByCurrency(currency);
		} else {
			final File currencyPairFile = fileManager.getFileByName(fromDate, toDate, currency);
			fileNameCurrencyPair = currencyPairFile.getAbsolutePath();
		}

		final TimeSeries series = csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair);
		return series;
	}

	private void processTick(final String currencyPair, final TradingRecord tradingRecord, final TimeSeries series, final Strategy strategy,
							 final Rule stopLossRule, final int curIndex) {

		final Tick tick = series.getTick(curIndex);

		if (checkForStopLoss) {
			processStopLoss(tradingRecord, curIndex, tick, stopLossRule);
		}

		final  boolean enterIndicated = processEnterStrategy(strategy, curIndex, tradingRecord, tick, currencyPair);

		if (!enterIndicated) {
			processExitStrategy(strategy, curIndex, tradingRecord, tick, currencyPair);
		}


	}

	/**
	 * Process enter strategy and return true if an enter was indicated
	 * @param strategy
	 * @param curIndex
	 * @param tradingRecord
	 * @param tick
	 * @param currencyPair
	 * @return True if enter indicated, does not necessarily mean the trade succesfully exited
	 */
	private boolean processEnterStrategy(final Strategy strategy, final int curIndex, final TradingRecord tradingRecord, final Tick tick, final String currencyPair) {
		if (strategy.shouldEnter(curIndex, tradingRecord)) {
			//Strategy should enter
			System.out.println("Strategy should ENTER on " + curIndex);

			Decimal numberToBuy = determineTradeAmount(tradingRecord, tick.getClosePrice());

			boolean entered = enterTrade(currencyPair, tradingRecord, tick.getClosePrice(), curIndex, numberToBuy);

			if (entered) {
				Order entry = tradingRecord.getLastEntry();
				System.out.println("Entered on " + entry.getIndex()
						+ " (price=" + entry.getPrice().toDouble()
						+ ", amount=" + entry.getAmount().toDouble() + ")");
			}
			return true;
		}
		return  false;
	}

	/**
	 * Process exit strategy and return true if an exit was indicated
	 * @param strategy
	 * @param curIndex
	 * @param tradingRecord
	 * @param tick
	 * @param currencyPair
	 * @return True if exit indicated, does not necessarily mean the trade succesfully exited
	 */
	private boolean processExitStrategy(final Strategy strategy, final int curIndex, final TradingRecord tradingRecord, final Tick tick, final String currencyPair) {
		if (strategy.shouldExit(curIndex, tradingRecord)) {
			//Strategy should exit
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
			return  true;
		}
		return  false;
	}

	private void processStopLoss(final TradingRecord tradingRecord, final int curIndex, final Tick tick, final Rule stopLossRule) {
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

	private TimeSeries getConversionCurrencySeries(final List<String> currencyPairList, final String fromDate, final String toDate) {
		TimeSeries series = null;
		for (final String currency : currencyPairList) {
			if (currency.equalsIgnoreCase(CONVERSION_CURRENCY)) {
				series = loadTimeSeries(currency, fromDate, toDate);
			}
		}
		return series;
	}

	private boolean enterTrade(final String currencyPair, final TradingRecord tradingRecord, final Decimal closePrice,
							   final int curIndex, final Decimal numberToBuy) {

		boolean entered = false;
		if (!liveTradeMode) {
			entered = tradingRecord.enter(curIndex, closePrice, numberToBuy);
		} else {

			final Decimal buyPriceDecimal = closePrice;

			final BigDecimal buyPrice = BigDecimal.valueOf(buyPriceDecimal.toDouble());

			poloniexTraderClient.buy(currencyPair, buyPrice, BigDecimal.valueOf(numberToBuy.toDouble()), conversionTimeSeries);

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

			poloniexTraderClient.sell(currencyPair, sellPrice, BigDecimal.valueOf(numberToSell.toDouble()), conversionTimeSeries);

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

}
