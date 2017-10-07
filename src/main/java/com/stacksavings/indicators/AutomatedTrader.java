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

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

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

	private List<String> currenciesEndingWithLoss;
	private Map<String, List<Decimal>> currencyTotals;


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


			for (String currency : currencyPairList)
			{
				if (skipCurrencyNonTradingReason(currency) || (parameters.getCurrencySkipList() != null && parameters.getCurrencySkipList().contains(currency))) {
					continue;
				}

				try {

					if (poloniexTraderClient.areOpenPoloniexOrders(currency, null)) {
						//TODO this logic is very rough, need to figure a lot more on this as there could be orders
						//that stay open a long time so we need logic to potentially cancel the order, etc, for now just skip
						//if it has open poloniex order as we can't do another trade while it's open
						continue;
					}

					final TimeSeries series = loadTimeSeries(currency, parameters.isUseConversionSeries());

					parameters.getStrategyHolder().setup(series);

					// Initializing the trading history
					final TradingRecord tradingRecord = new TradingRecord(); //BaseTradingRecord();

					if (parameters.isLiveTradeMode()) {
						runLiveTrade(tradingRecord, currency, series);
					} else {
						runBacktest(series, currency, tradingRecord, activePositionsAtIndexTracker);
					}
				} catch (final Exception e) {
					loggerHelper.getDefaultLogger().error("Exception encountered for currency " + currency + ", stack trace follows: ", e);
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

	private void runLiveTrade(final  TradingRecord tradingRecord, final String currency, final TimeSeries series) {
		synchTradeAccountRecords(tradingRecord, currency);

		//process only the most recent tick as that is the only one that is relevant in real time trading
		processTick(currency, tradingRecord, series, series.getEnd());
	}

	private void runBacktest(final TimeSeries series, final String currency, final  TradingRecord tradingRecord, final Map<Integer, Integer> activePositionsAtIndexTracker) {
		for (int i = 0; i < series.getTickCount(); i++) {

			processTick(currency, tradingRecord, series, i);

			if (!tradingRecord.isClosed()) {
				final Integer curActiveCount = activePositionsAtIndexTracker.get(i);
				final Integer newActiveCount = curActiveCount != null ? curActiveCount + 1 : 1;
				activePositionsAtIndexTracker.put(i, newActiveCount);
			}

		}

		Decimal endingFunds = parameters.getInitialCurrencyAmount();
		if (tradingRecord.getLastExit() != null) {
			endingFunds = tradingRecord.getLastExit().getPrice().multipliedBy(tradingRecord.getLastExit().getAmount());
		}

		final double totalProfit = new TotalProfitCriterion().calculate(series, tradingRecord);

		final Decimal totalPercentChange = calculatePercentChange(parameters.getInitialCurrencyAmount(), endingFunds);
		if (totalPercentChange.isNegative()) {
			currenciesEndingWithLoss.add(currency);
		}

		loggerHelper.logCurrencySummaryRow(totalProfit, parameters.getInitialCurrencyAmount(), endingFunds, totalPercentChange);

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
		if (parameters.isLiveTradeMode()) {
			fileNameCurrencyPair = fileManager.getFileNameByCurrency(currency);
		} else {
			final File currencyPairFile = fileManager.getFileByName(parameters.getFromDate(), parameters.getToDate(), currency);
			fileNameCurrencyPair = currencyPairFile.getAbsolutePath();
		}

		final TimeSeries series = csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair, useConversionSeries, conversionTimeSeries);
		return series;
	}

	private void processTick(final String currencyPair, final TradingRecord tradingRecord, final TimeSeries series, final int curIndex) {

		final Tick tick = series.getTick(curIndex);

		processStopLoss(tradingRecord, series, curIndex, tick);

		final boolean enterIndicated = processEnterStrategy(curIndex, tradingRecord, tick, currencyPair, series);

		if (!enterIndicated) {
			processExitStrategy(curIndex, tradingRecord, tick, currencyPair);
		}

	}

	/**
	 * Process enter strategy and return true if an enter was indicated
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

				Decimal numberToBuy = determineTradeAmount(tradingRecord, tick.getClosePrice());

				boolean entered = enterTrade(currencyPair, tradingRecord, tick.getClosePrice(), curIndex, numberToBuy);

				if (entered) {
					Order entry = tradingRecord.getLastEntry();
					loggerHelper.logTickRow(currencyPair,"ENTER", entry.getIndex(), entry.getPrice().toDouble(), entry.getAmount().toDouble());
				}
				return true;
			}
		}
		return  false;
	}

	/**
	 * Process exit strategy and return true if an exit was indicated
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

	private boolean enterTrade(final String currencyPair, final TradingRecord tradingRecord, final Decimal closePrice,
							   final int curIndex, final Decimal numberToBuy) {

		boolean entered = false;
		if (!parameters.isLiveTradeMode()) {
			entered = enterTrade(tradingRecord, closePrice, curIndex, numberToBuy);
		} else {

			final Decimal buyPriceDecimal = closePrice;

			final BigDecimal buyPrice = BigDecimal.valueOf(buyPriceDecimal.toDouble());

			poloniexTraderClient.buy(currencyPair, buyPrice, BigDecimal.valueOf(numberToBuy.toDouble()), conversionTimeSeries);

			//TODO, figure what to do on this, we aren't necessarily going to know right away if a trade actually was processed for real time trading
			entered = enterTrade(tradingRecord, closePrice, curIndex, numberToBuy);

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

	private boolean enterTrade(final TradingRecord tradingRecord, final Decimal closePrice,
							   final int curIndex, final Decimal numberToBuy) {

		final Decimal buyPrice = applyBuyFee(closePrice);

		final boolean entered = tradingRecord.enter(curIndex, buyPrice, numberToBuy);
		return entered;

	}

	private boolean exitTrade(final TradingRecord tradingRecord, final Decimal closePrice,
							  final int curIndex, final Decimal numberToSell) {

		final Decimal sellPrice = applySellFee(closePrice);

		final boolean exited = tradingRecord.exit(curIndex, sellPrice, numberToSell);
		return exited;
	}

	private Decimal applyBuyFee (final Decimal price) {
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

	//TODO this seems to need to use the starting funds variable, hard-coding this to use 'initialSpendAmtPerCurrency' is confusing and probably not optimal, this variable is deprecated
	private Decimal determineTradeAmount(final TradingRecord tradingRecord, final Decimal currentPrice) {
		boolean isFirstTrade = true;
		if (tradingRecord == null || tradingRecord.getTradeCount() > 0) {
			isFirstTrade = false;
		}

		Decimal availableFunds = Decimal.ZERO;

		if (isFirstTrade) {
			availableFunds = parameters.getInitialCurrencyAmount();
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
