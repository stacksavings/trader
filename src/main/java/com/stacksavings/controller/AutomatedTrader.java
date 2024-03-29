package com.stacksavings.controller;

import com.stacksavings.Parameter.Parameters;
import com.stacksavings.client.api.PoloniexClientApi;
import com.stacksavings.strategies.StrategyHolder;
import com.stacksavings.tradingrecord.TradingRecordCollection;
import com.stacksavings.tradingrecord.holders.TradingRecordHolder;
import com.stacksavings.tradingrecord.holders.TradingRecordHolderFactory;
import com.stacksavings.utils.GenericUtils;
import com.stacksavings.utils.LoggerHelper;
import eu.verdelhan.ta4j.*;
import eu.verdelhan.ta4j.indicators.trackers.AverageDirectionalMovementIndicator;
import java.util.*;

/**
 * Controller for trading
 * @author Rickd
 *
 */
public class AutomatedTrader {

	private PoloniexClientApi poloniexClientApi;

	private Parameters parameters;

	private LoggerHelper loggerHelper;

	private TradingRecordCollection tradingRecordCollection;

	private List<String> currenciesEndingWithLoss;
	private Map<String, List<Decimal>> currencyTotals;


	/**
	 *
	 */
	public AutomatedTrader(final Parameters parameters)
	{

		loggerHelper = LoggerHelper.getInstance();
		poloniexClientApi = PoloniexClientApi.getInstance();
		this.parameters = parameters;

		currencyTotals = new HashMap<String, List<Decimal>>();
		currenciesEndingWithLoss = new ArrayList<String>();

	}

	public void run() throws Exception {

		loggerHelper.logParameters(parameters);

		final List<String> currencyPairListTmp = poloniexClientApi.returnCurrencyPair(parameters.getConversionCurrency());

		final List<String> currencyPairList = GenericUtils.filterCurrencyList(currencyPairListTmp, parameters.getCurrencyIncludeList() , parameters.getCurrencySkipList());

		if (parameters.isLiveTradeMode()) {
			//logger.trace("******* BEGIN live trading iteration *******");
		}

		if(currencyPairList != null && currencyPairList.size() > 0) {

			tradingRecordCollection = new TradingRecordCollection();
			tradingRecordCollection.init(parameters.getConversionCurrency(), currencyPairList, parameters.getStrategyHolder(), parameters.getFromDate(), parameters.getToDate());

			parameters.getAllocator().init(loggerHelper, tradingRecordCollection.getConversionTimeSeries(), currencyPairList);

			for (final String currencyPair : currencyPairList) {

				//TODO this seems to be possibly bad design, as it requires knowing implementation details about the other class
				Decimal stopLossRatio = null;
				if (parameters.shouldProccessStopLoss()) {
					stopLossRatio = parameters.getStopLossRatio();
				}

				//TODO having this take the conversionTimeSeries from tradingRecordCollection does not seem to be proper design
				final TradingRecordHolder tradingRecordHolder = TradingRecordHolderFactory.createTradingRecordHolder( currencyPair, stopLossRatio, parameters.getFeeAmount(), parameters.getFromDate(),  parameters.getToDate(),
				parameters.getStrategyHolder(),  tradingRecordCollection.getConversionTimeSeries(),  parameters.isLiveTradeMode(), parameters.isUseCachedBuySellSignals());

				tradingRecordCollection.addTradingRecordHolder(currencyPair, tradingRecordHolder);

			}

			//TODO there are more elegant ways to do this, tradingRecordCollection should't have to give out the info about total iterations, it could just give out the iter from each call of processIteration, for example
			for (int i = 0; i < tradingRecordCollection.getTotalIterations(); i++) {

				tradingRecordCollection.processIteration(i);

				//TODO this needs to be re-worked to be more encapsulated within the allocator classs
				parameters.getAllocator().processAccountingForSales(tradingRecordCollection.getCurIterSellTradingRecordHolders(), i);
				parameters.getAllocator().processTickBuys( tradingRecordCollection.getCurIterBuyTradingRecordHolders(), null, i);
				parameters.getAllocator().iterationFinalAccountingProcessing(i);

			}

			if (!parameters.isLiveTradeMode()) {
				double totalProfit = 0.00;
				for (String currencyPair : currencyPairList) {
					//TODO this should be re-factored to be more obejct oriented, this logic needs to be part of it's own class type
					final TradingRecord tradingRecord = tradingRecordCollection.getTradingRecordHolder(currencyPair).getTradingRecord();

					//this doesn't take into the allocator account balances
					totalProfit = totalProfit + logBackTestCurrencyTotals(currencyPair, tradingRecord);
				}

				System.out.println("allocator btc balance: " + parameters.getAllocator().getBtcBalance());
				System.out.println("allocator conversion series balance: " + parameters.getAllocator().getConversionCurrencyBalance());
			}


			//TODO refactor to be part of appropriat class type / new class type
			if (!parameters.isLiveTradeMode()) {
				calculateOverallGainLoss(currencyTotals, currenciesEndingWithLoss);

				//TODO this has to be refactored as it was causing a large performance decrease
/*				for ( final Map.Entry<Integer, Integer> entry : activePositionsAtIndexTracker.entrySet()){
					loggerHelper.logTickCombinedSummaryRow(entry.getKey(), entry.getValue());
				}*/
			}
		} 
		else {
			loggerHelper.getDefaultLogger().error("Date missing, unable to process");
		} if (parameters.isLiveTradeMode()) {
			//logger.trace("******* END live trading iteration *******");
		}

	}


	//TODO this method is most likely not accurate as the allocation strategy doesn't guarantee it will use all funds from a sale to buy more of that currency later on so it could show a loss for that currency which may be inaccurate
	//TODO this needs to be re-worked, it needs to use an object that wraps a time series and holds data about every trade made and can also track the currrent value, for example,
	//if a trade is still active the only way to track this is from the time series to get the latest price
	private double logBackTestCurrencyTotals(final String currency, final TradingRecord tradingRecord) {
		Decimal endingFunds = parameters.getInitialCurrencyAmount();

		Decimal totalProfit = Decimal.ZERO;
		if (tradingRecord != null && tradingRecord.getLastExit() != null) {
			endingFunds = tradingRecord.getLastExit().getPrice().multipliedBy(tradingRecord.getLastExit().getAmount());
		}
		//TODO this is a hack, it is not accurate, as it does not take the latest tick price from the series
		else if (tradingRecord != null && tradingRecord.getLastEntry() != null) {
			endingFunds = tradingRecord.getLastEntry().getPrice().multipliedBy(tradingRecord.getLastEntry().getAmount());
		}

		totalProfit = endingFunds.minus(parameters.getInitialCurrencyAmount());
		final Decimal totalPercentChange = calculatePercentChange(parameters.getInitialCurrencyAmount(), endingFunds);
		if (totalPercentChange.isNegative()) {
			currenciesEndingWithLoss.add(currency);
		}

		loggerHelper.logCurrencySummaryRow(currency, totalProfit.toDouble(), parameters.getInitialCurrencyAmount(), endingFunds, totalPercentChange);

		currencyTotals.put(currency, Arrays.asList(parameters.getInitialCurrencyAmount(), endingFunds));

		return totalProfit.toDouble();
	}

	//TODO re-work to be part of appropriate class type, this should be a strategy
	/**
	 * Expirementing with indicators that can give some sort of threshold to determine whether a trade is actually worth making, related to the movement direction
	 * @param series
	 * @param index
	 * @return
	 */
/*	private boolean checkIfAboveExperimentalIndicatorThreshold(final TimeSeries series, final int index) {
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
	}*/

	//TODO re-work to be part of an appropriate class type
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
