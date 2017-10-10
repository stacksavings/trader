package com.stacksavings.indicators;

import com.stacksavings.Parameter.Parameters;
import com.stacksavings.client.api.PoloniexClientApi;
import com.stacksavings.loaders.CsvTicksLoader;
import com.stacksavings.utils.FileManager;
import com.stacksavings.utils.LoggerHelper;
import com.stacksavings.utils.PoloniexTraderClient;
import com.stacksavings.utils.PropertiesUtil;
import eu.verdelhan.ta4j.*;
import eu.verdelhan.ta4j.analysis.criteria.TotalProfitCriterion;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.AverageDirectionalMovementIndicator;
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

	private CsvTicksLoader csvTicksLoader;
	private PoloniexClientApi poloniexClientApi;
	private FileManager fileManager;
	private PoloniexTraderClient poloniexTraderClient;

	private Parameters parameters;

	private PropertiesUtil propertiesUtil;

	private LoggerHelper loggerHelper;

	private TimeSeries conversionTimeSeries;

	private Map<String, TradingRecord> backTestTradingRecords;

	private List<String> currenciesEndingWithLoss;
	private Map<String, List<Decimal>> currencyTotals;
	private Map<String, TimeSeries> timeSeriesHolder;

	private enum IterateCurrencyMode {
		EXIT, ENTER;
	}


	/**
	 *
	 */
	public AutomatedTrader(final Parameters parameters)
	{

		loggerHelper = new LoggerHelper();
		csvTicksLoader = CsvTicksLoader.getInstance();
		poloniexClientApi = PoloniexClientApi.getInstance();
		fileManager = FileManager.getInstance();
		propertiesUtil = PropertiesUtil.getInstance();
		poloniexTraderClient = PoloniexTraderClient.getInstance();
		this.parameters = parameters;

		currencyTotals = new HashMap<String, List<Decimal>>();
		currenciesEndingWithLoss = new ArrayList<String>();
		timeSeriesHolder = new HashMap<String, TimeSeries>();
		backTestTradingRecords = new HashMap<String, TradingRecord>();

		parameters.getAllocator().init(loggerHelper);
	}
	

	public void run() throws Exception {

		loggerHelper.logParameters(parameters);

		final List<String> currencyPairList = poloniexClientApi.returnCurrencyPair(parameters.getConversionCurrency());

		if (parameters.isLiveTradeMode()) {
			//logger.trace("******* BEGIN live trading iteration *******");
		}


		if(currencyPairList != null && currencyPairList.size() > 0)
		{
			if (parameters.isUseConversionSeries()) {
				conversionTimeSeries = getConversionCurrencySeries(currencyPairList);
				if (conversionTimeSeries == null) {
					throw new Exception("Conversion time series came back as null for " + parameters.getConversionCurrency() + ", cannot proceed without this");
				}
			}

			//For back-testing only
			final Map<Integer, Integer> activePositionsAtIndexTracker = new HashMap<Integer, Integer>();

			//All the series should be the same length, so just use the conversion series as the iterator
			int totalIterations = 1;
			if (!parameters.isLiveTradeMode()) {
				totalIterations = conversionTimeSeries.getTickCount();
			}

			for (int i = 0; i < totalIterations; i++) {
				//Exits must be performed first, as the allocation strategy would then decide which ENTER trades are allowed based on the available funds
				iterateCurrencies(IterateCurrencyMode.EXIT, currencyPairList, activePositionsAtIndexTracker, i);

				iterateCurrencies(IterateCurrencyMode.ENTER, currencyPairList, activePositionsAtIndexTracker, i);

			}


			if (!parameters.isLiveTradeMode()) {
				for (String currency : currencyPairList) {
					logBackTestCurrencyTotals(currency, backTestTradingRecords.get(currency), timeSeriesHolder.get(currency));
				}
			}



			if (!parameters.isLiveTradeMode()) {
				calculateOverallGainLoss(currencyTotals, currenciesEndingWithLoss);

				for ( final Map.Entry<Integer, Integer> entry : activePositionsAtIndexTracker.entrySet()){
					loggerHelper.logTickCombinedSummaryRow(entry.getKey(), entry.getValue());
				}
			}
		} 
		else {
			loggerHelper.getDefaultLogger().error("Date missing, unable to process");
		} if (parameters.isLiveTradeMode()) {
			//logger.trace("******* END live trading iteration *******");
		}

	}

	private void iterateCurrencies(final IterateCurrencyMode iterateCurrencyMode, final List<String> currencyPairList, final Map<Integer, Integer> activePositionsAtIndexTracker, final int iter) {

		final Map<String, Tick> buyTicks = new HashMap<String, Tick>();
		final Map<String, TradingRecord> buyTradingRecords = new HashMap<String, TradingRecord>();

		for (String currency : currencyPairList) {
			if (skipCurrencyNonTradingReason(currency) || (parameters.getCurrencySkipList() != null && parameters.getCurrencySkipList().contains(currency))) {
				continue;
			}

			try {

				final TimeSeries series = loadTimeSeries(currency, parameters.isUseConversionSeries());
				parameters.getStrategyHolder().setup(series);

				final TradingRecord tradingRecord = getTradingRecord(currency);

				if (iterateCurrencyMode == IterateCurrencyMode.EXIT) {
					processTickExit(currency, tradingRecord, series, iter);
				} else if (iterateCurrencyMode == IterateCurrencyMode.ENTER) {

					final Tick tick = series.getTick(iter);
					final boolean enterIndicated = processEnterStrategy(iter, tradingRecord, tick, currency, series);

					if (enterIndicated) {
						buyTicks.put(currency, tick);
						buyTradingRecords.put(currency, tradingRecord);
					} else {
						//This tick / trading record pair is finished so record the active position, if applicable
						updateActivePositionsAtIndex(tradingRecord, activePositionsAtIndexTracker, iter);
					}
				}

			} catch (final Exception e) {
				loggerHelper.getDefaultLogger().error("Exception encountered for currency " + currency + ", stack trace follows: ", e);
			}
		}

		parameters.getAllocator().processTickBuys( buyTicks, buyTradingRecords, iter);


	}

	private void updateActivePositionsAtIndex(final TradingRecord tradingRecord, final Map<Integer, Integer> activePositionsAtIndexTracker, final int iter) {
		if (!parameters.isLiveTradeMode()) {
			if (!tradingRecord.isClosed()) {
				final Integer curActiveCount = activePositionsAtIndexTracker.get(iter);
				final Integer newActiveCount = curActiveCount != null ? curActiveCount + 1 : 1;
				activePositionsAtIndexTracker.put(iter, newActiveCount);
			}
		}
	}

	private TradingRecord getTradingRecord(final String currency) {
		TradingRecord tradingRecord = null;
		if (parameters.isLiveTradeMode()) {
			tradingRecord = new TradingRecord();

			synchTradeAccountRecords(tradingRecord, currency);

		} else {
			tradingRecord = backTestTradingRecords.get(currency);
			if (tradingRecord == null) {
				tradingRecord = new TradingRecord();
				backTestTradingRecords.put(currency, tradingRecord);
			}
		}
		return tradingRecord;
	}

	private void logBackTestCurrencyTotals(final String currency, final TradingRecord tradingRecord, final TimeSeries series) {
		Decimal endingFunds = parameters.getInitialCurrencyAmount();

		double totalProfit = 0.00;
		if (tradingRecord != null && tradingRecord.getLastExit() != null) {
			endingFunds = tradingRecord.getLastExit().getPrice().multipliedBy(tradingRecord.getLastExit().getAmount());

			totalProfit = new TotalProfitCriterion().calculate(series, tradingRecord);
		}

		final Decimal totalPercentChange = calculatePercentChange(parameters.getInitialCurrencyAmount(), endingFunds);
		if (totalPercentChange.isNegative()) {
			currenciesEndingWithLoss.add(currency);
		}

		loggerHelper.logCurrencySummaryRow(currency, totalProfit, parameters.getInitialCurrencyAmount(), endingFunds, totalPercentChange);

		currencyTotals.put(currency, Arrays.asList(parameters.getInitialCurrencyAmount(), endingFunds));
	}


	/**
	 * Expirementing with indicators that can give some sort of threshold to determine whether a trade is actually worth making, related to the movement direction
	 * @param series
	 * @param index
	 * @return
	 */
	private boolean checkIfAboveExperimentalIndicatorThreshold(final TimeSeries series, final int index) {
		if (!parameters.isApplyExperimentalIndicator()) {
			return true;
		}
		final int timeFrame = 21;
		final AverageDirectionalMovementIndicator admIndicator = new AverageDirectionalMovementIndicator(series, timeFrame);
		final Decimal admValue = admIndicator.getValue(index);

		if (admValue.isGreaterThan(Decimal.valueOf(20.0))) {
			return true;
		}
		return false;
	}

	/**
	 * Skip a currency for a reason other than that it is determined to not be optimal to trade. An example is that we are basing buy / sell off of usdt so we can't also trade it. This differs
	 * from currencies that we skip due to having determined that they may not be optimal to trade from backtesting, for example.
	 * @param currency
	 * @return
	 */
	private boolean skipCurrencyNonTradingReason(final String currency) {
		if (StringUtils.startsWithIgnoreCase(currency, parameters.getConversionCurrency())) {
			return true;
		}
		return false;
	}

	/**
	 * Load a time series from a currency, optionally a fromDate and toDate
	 * @param currency
	 * @return
	 */
	private TimeSeries loadTimeSeries(final String currency, final boolean useConversionSeries) {
		String fileNameCurrencyPair = null;
		TimeSeries timeSeries = null;
		if (parameters.isLiveTradeMode()) {
			fileNameCurrencyPair = fileManager.getFileNameByCurrency(currency);
		} else {

			timeSeries = timeSeriesHolder.get(currency);
			if (timeSeries != null) {
				return timeSeries;
			}
			final File currencyPairFile = fileManager.getFileByName(parameters.getFromDate(), parameters.getToDate(), currency);
			fileNameCurrencyPair = currencyPairFile.getAbsolutePath();
		}

		timeSeries = csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair, useConversionSeries, conversionTimeSeries);
		if (!parameters.isLiveTradeMode()) {
			timeSeriesHolder.put(currency, timeSeries);
		}
		return timeSeries;
	}

	private void processTickExit(final String currencyPair, final TradingRecord tradingRecord, final TimeSeries series, final int curIndex) {

		final Tick tick = series.getTick(curIndex);

		processStopLoss(tradingRecord, series, curIndex, tick);
		processExitStrategy(curIndex, tradingRecord, tick, currencyPair);


	}


	/**
	 * Process enter strategy and return true if an enter was indicated, does not actually enter the trade
	 * @param curIndex
	 * @param tradingRecord
	 * @param tick
	 * @param currencyPair
	 * @return True if enter indicated, does not necessarily mean the trade succesfully exited
	 */
	private boolean processEnterStrategy(final int curIndex, final TradingRecord tradingRecord, final Tick tick, final String currencyPair, final TimeSeries series) {
		//TODO this has to be re-worked to probably build a strategy each time because it needs to set the current price adjusted for the fee, since a decision to buy / sell, must take into account the fee
		if (parameters.getStrategyHolder().shouldEnter(curIndex, tradingRecord)) {

			boolean aboveExperimentalIndicator = checkIfAboveExperimentalIndicatorThreshold(series, curIndex);
			if (aboveExperimentalIndicator) {

				return true;
			}
		}
		return  false;
	}

	/**
	 * Process exit strategy and return true if an exit was indicated also processes the actual exit trade
	 * @param curIndex
	 * @param tradingRecord
	 * @param tick
	 * @param currencyPair
	 * @return True if exit indicated, does not necessarily mean the trade succesfully exited
	 */
	private boolean processExitStrategy(final int curIndex, final TradingRecord tradingRecord, final Tick tick, final String currencyPair) {
		//TODO this has to be re-worked to probably build a strategy each time because it needs to set the current price adjusted for the fee, since a decision to buy / sell, must take into account the fee
		if (parameters.getStrategyHolder().shouldExit(curIndex, tradingRecord)) {

			if (tradingRecord != null && tradingRecord.getLastEntry() != null) {
				Decimal exitAmount = tradingRecord.getLastEntry().getAmount();

				boolean exited = exitTrade(currencyPair, tradingRecord, tick.getClosePrice(), curIndex, exitAmount);

				if (exited) {
					Order exit = tradingRecord.getLastExit();
					loggerHelper.logTickRow(currencyPair,"EXIT", exit.getIndex(), exit.getPrice().toDouble(), exit.getAmount().toDouble());
				}
			}
			return  true;
		}
		return  false;
	}

	//TODO this would need to account for fees if its going to be used, as fees have to be considered in all buying / selling decisions
	private void processStopLoss(final TradingRecord tradingRecord, final TimeSeries series, final int curIndex, final Tick tick) {
		//If stop loss is triggered, take this out of any future trading
		//This is an experiment to try removing ones that trigger a stop loss, the idea is that we may want to decide not to trade
		//certain currencies at all, as they may be too volatile for an algorithm
		if (parameters.shouldProccessStopLoss() && tradingRecord != null && !tradingRecord.isClosed()) {

			final ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
			final Rule stopLossRule = new StopLossRule(closePrice, parameters.getStopLossRatio());

			final Decimal lastEntryPrice = tradingRecord.getLastEntry().getPrice();
			boolean shouldStopLoss = (stopLossRule == null ? false : stopLossRule.isSatisfied(curIndex, tradingRecord));
			if (shouldStopLoss) {
				boolean exited = tradingRecord.exit(curIndex, tick.getClosePrice(), tradingRecord.getLastEntry().getAmount());
				if (exited) {
					Order exit = tradingRecord.getLastExit();
					loggerHelper.getDefaultLogger().trace("STOP LOSS TRIGGERED, TRADING HALTED for loss of %: " +
							calculatePercentChange(lastEntryPrice, exit.getPrice()) + " on index: " + exit.getIndex()
							+ " (price=" + exit.getPrice().toDouble()
							+ ", amount=" + exit.getAmount().toDouble() + ")");
					return;
				}
			}
		}
	}

	private TimeSeries getConversionCurrencySeries(final List<String> currencyPairList) {
		TimeSeries series = null;
		for (final String currency : currencyPairList) {
			if (currency.equalsIgnoreCase(parameters.getConversionCurrency())) {
				series = loadTimeSeries(currency, false);
			}
		}
		return series;
	}

	public static boolean enterTrade(final String currencyPair, final TradingRecord tradingRecord, final Decimal closePrice,
							   final int curIndex, final Decimal numberToBuy, final Parameters parameters) {

		boolean entered = false;
		if (!parameters.isLiveTradeMode()) {
			entered = enterTrade(tradingRecord, closePrice, curIndex, numberToBuy, parameters);
		} else {

			final Decimal buyPriceDecimal = closePrice;

			final BigDecimal buyPrice = BigDecimal.valueOf(buyPriceDecimal.toDouble());

			//TODO need to refactor this for live trading mode
			//poloniexTraderClient.buy(currencyPair, buyPrice, BigDecimal.valueOf(numberToBuy.toDouble()), conversionTimeSeries);

			//TODO, figure what to do on this, we aren't necessarily going to know right away if a trade actually was processed for real time trading
			entered = enterTrade(tradingRecord, closePrice, curIndex, numberToBuy, parameters);

		}

		return entered;
	}

	private boolean exitTrade(final String currencyPair, final TradingRecord tradingRecord, final Decimal closePrice,
							   final int curIndex, final Decimal numberToSell) {

		boolean exited = false;
		if (!parameters.isLiveTradeMode()) {
			exited = exitTrade(tradingRecord, closePrice, curIndex, numberToSell);
		} else {

			final Decimal sellPriceDecimal = closePrice;

			final BigDecimal sellPrice = BigDecimal.valueOf(sellPriceDecimal.toDouble());

			poloniexTraderClient.sell(currencyPair, sellPrice, BigDecimal.valueOf(numberToSell.toDouble()), conversionTimeSeries);

			//TODO, figure what to do on this, we aren't necessarily going to know right away if a trade actually was processed for real time trading
			exited = exitTrade(tradingRecord, closePrice, curIndex, numberToSell);

		}

		return exited;
	}

	public static boolean enterTrade(final TradingRecord tradingRecord, final Decimal closePrice,
							   final int curIndex, final Decimal numberToBuy, final Parameters parameters) {

		final Decimal buyPrice = applyBuyFee(closePrice, parameters);

		final boolean entered = tradingRecord.enter(curIndex, buyPrice, numberToBuy);
		return entered;

	}

	private boolean exitTrade(final TradingRecord tradingRecord, final Decimal closePrice,
							  final int curIndex, final Decimal numberToSell) {

		final Decimal sellPrice = applySellFee(closePrice);

		final boolean exited = tradingRecord.exit(curIndex, sellPrice, numberToSell);
		return exited;
	}

	public static Decimal applyBuyFee (final Decimal price, final Parameters parameters) {
		final Decimal buyPrice = (price.multipliedBy(parameters.getFeeAmount())).plus(price);
		return buyPrice;
	}

	private Decimal applySellFee (final Decimal price) {
		final Decimal sellPrice = price.minus((price.multipliedBy(parameters.getFeeAmount())));
		return sellPrice;
	}

	/**
	 * When the program is run in trading mode it has to first check the poloniex orders from before and at least update the most recent one
	 * as a trading record trade so that we know the current status of that currency
	 * @param tradingRecord
	 */
	private void synchTradeAccountRecords(final TradingRecord tradingRecord, final String currency) {

		poloniexTraderClient.createTradingRecordFromPoloniexTrade(currency, tradingRecord);
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

		loggerHelper.logSummaryRow(start, end, calculatePercentChange(start, end));

		loggerHelper.getDefaultLogger().trace("Currencies ending with loss:");
		for (final String currency : currenciesEndingWithLoss) {
			loggerHelper.getDefaultLogger().trace(currency);
		}

	}

	private Decimal calculatePercentChange(final Decimal startingFunds, final Decimal endingFunds) {
		final Decimal percentChange = endingFunds.minus(startingFunds).dividedBy(startingFunds).multipliedBy(Decimal.valueOf(100));
		return percentChange;

	}

}
