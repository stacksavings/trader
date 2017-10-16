package com.stacksavings.tradingrecord.holders;

import com.stacksavings.Parameter.Parameters;
import com.stacksavings.loaders.CsvTicksLoader;
import com.stacksavings.strategies.StrategyHolder;
import com.stacksavings.utils.FileManager;
import com.stacksavings.utils.GenericUtils;
import com.stacksavings.utils.LoggerHelper;
import com.stacksavings.utils.PoloniexTraderClient;
import eu.verdelhan.ta4j.*;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.trading.rules.StopLossRule;
import java.util.*;

public class TradingRecordHolder {

    protected TimeSeries timeSeries;
    protected TimeSeries conversionTimeSeries;
    protected String currencyPair;

    protected StrategyHolder strategyHolder;
    protected Decimal feeAmount;

    protected PoloniexTraderClient poloniexTraderClient;

    protected LoggerHelper loggerHelper;

    protected CsvTicksLoader csvTicksLoader;
    protected FileManager fileManager;

    protected TradingRecord tradingRecord;

    protected Decimal stopLossRatio;

    protected int curIter;

    /**
     * When the program is run in trading mode it has to first check the poloniex orders from before and at least update the most recent one
     * as a trading record trade so that we know the current status of that currency
     * @param tradingRecord
     */
    private void synchTradeAccountRecords(final TradingRecord tradingRecord, final String currency) {

        poloniexTraderClient.createTradingRecordFromPoloniexTrade(currency, tradingRecord);
    }

    public TradingRecordHolder() {


    }

    public void init(final String currencyPair, final TimeSeries conversionTimeSeries, final Decimal stopLossRatio, final String fromDate, final String toDate,
                     final StrategyHolder strategyHolder, final Decimal feeAmount) {

        this.currencyPair = currencyPair;
        this.conversionTimeSeries = conversionTimeSeries;
        this.stopLossRatio = stopLossRatio;
        this.strategyHolder = strategyHolder;
        this.feeAmount = feeAmount;

        fileManager = FileManager.getInstance();
        csvTicksLoader = CsvTicksLoader.getInstance();

        tradingRecord = new TradingRecord();

        loggerHelper = LoggerHelper.getInstance();

        timeSeries = GenericUtils.loadTimeSeries(currencyPair, fromDate, toDate, true, this.conversionTimeSeries, fileManager, csvTicksLoader);

    }


    //TODO implement this, this is just for logging in back-testing
    protected void updateActivePositionsAtIndex(final TradingRecord tradingRecord, final Map<Integer, Integer> activePositionsAtIndexTracker, final int iter, final Parameters parameters) {
/*		if (!parameters.isLiveTradeMode()) {
			if (!tradingRecord.isClosed()) {
				final Integer curActiveCount = activePositionsAtIndexTracker.get(iter);
				final Integer newActiveCount = curActiveCount != null ? curActiveCount + 1 : 1;
				activePositionsAtIndexTracker.put(iter, newActiveCount);
			}
		}*/
    }




    public boolean processTickExit(final int curIter) {

        this.curIter = curIter;

        final Tick tick = timeSeries.getTick(curIter);

        boolean exitIndicated = processStopLoss(tick);
        if (!exitIndicated) {
            exitIndicated = processExitStrategy(curIter);
        }
        return exitIndicated;
    }


    protected boolean runEnterStrategy() {

/*        if (!parameters.isLiveTradeMode() && parameters.isUseCachedBuySellSignals()) {
            shouldEnter = buySellCache.get(currencyPair).get(curIndex).get("shouldenter");

        } else {*/
           boolean shouldEnter = strategyHolder.shouldEnter(curIter, null);

        return shouldEnter;

    }

    protected boolean runExitStrategy() {
/*        boolean shouldExit = false;
        if (!parameters.isLiveTradeMode() && parameters.isUseCachedBuySellSignals()) {
            shouldExit = buySellCache.get(currencyPair).get(curIndex).get("shouldexit");
        } else {*/
           boolean shouldExit = strategyHolder.shouldExit(curIter, null);

        return shouldExit;

    }


    /**
     * Process enter strategy and return true if an enter was indicated, does not actually enter the trade
     * @param curIter
     * @return True if enter indicated, does not necessarily mean the trade succesfully exited
     */
    public boolean processEnterStrategy(final int curIter) {

        this.curIter = curIter;

        final Tick tick = timeSeries.getTick(curIter);
        //only process if there is not already an open trade
        if (tradingRecord != null && tradingRecord.isClosed()) {
            if (runEnterStrategy()) {
                return true;
/*
                boolean aboveExperimentalIndicator = checkIfAboveExperimentalIndicatorThreshold(series, curIndex);
                if (aboveExperimentalIndicator) {
                    return true;
                }*/



            }
        }
        return  false;
    }

    /**
     * Process exit strategy and return true if an exit was indicated also processes the actual exit trade
     * @param curIter
     * @return True if exit indicated, does not necessarily mean the trade succesfully exited
     */
    protected boolean processExitStrategy(final int curIter) {
        this.curIter = curIter;

        final Tick tick = timeSeries.getTick(curIter);

        //only process if there is an open trade
        if (tradingRecord != null && !tradingRecord.isClosed()) {
            if (runExitStrategy()) {
                final Decimal exitAmount = tradingRecord.getCurrentTrade().getEntry().getAmount();
                boolean exited = exitTrade(currencyPair, tradingRecord, tick.getClosePrice(), curIter, exitAmount);

                if (exited) {
                    final Order exit = tradingRecord.getLastExit();
                    loggerHelper.logTickRow(currencyPair,"EXIT", exit.getIndex(), exit.getPrice().toDouble(), exit.getAmount().toDouble());
                }
                return  true;
            }
        }
        return  false;
    }



    public boolean enterTrade(final Decimal closePrice, final Decimal numberToBuy) {

        final Decimal buyPrice = GenericUtils.applyBuyFee(closePrice, feeAmount);

        final boolean entered = tradingRecord.enter(curIter, buyPrice, numberToBuy);
        return entered;

    }

    protected boolean exitTrade(final String currencyPair, final TradingRecord tradingRecord, final Decimal closePrice,
                              final int curIndex, final Decimal numberToSell) {

        boolean exited = exitTrade(tradingRecord, closePrice, curIndex, numberToSell);
        return exited;
    }

    protected boolean exitTrade(final TradingRecord tradingRecord, final Decimal closePrice,
                              final int curIndex, final Decimal numberToSell) {

        final Decimal sellPrice = GenericUtils.applySellFee(closePrice, feeAmount);

        final boolean exited = tradingRecord.exit(curIndex, sellPrice, numberToSell);
        return exited;
    }


    protected boolean processStopLoss(final Tick tick) {
        //If stop loss is triggered, take this out of any future trading
        //This is an experiment to try removing ones that trigger a stop loss, the idea is that we may want to decide not to trade
        //certain currencies at all, as they may be too volatile for an algorithm
        if (stopLossRatio != null && !tradingRecord.isClosed()) {

            final ClosePriceIndicator closePrice = new ClosePriceIndicator(timeSeries);
            final Rule stopLossRule = new StopLossRule(closePrice, stopLossRatio);

            final Decimal lastEntryPrice = tradingRecord.getLastEntry().getPrice();
            boolean shouldStopLoss = (stopLossRule == null ? false : stopLossRule.isSatisfied(curIter, tradingRecord));
            if (shouldStopLoss) {
                boolean exited = tradingRecord.exit(curIter, tick.getClosePrice(), tradingRecord.getLastEntry().getAmount());
                if (exited) {
                    Order exit = tradingRecord.getLastExit();
                    //TOOD re-work so this logging can be called
/*                    loggerHelper.getDefaultLogger().trace("STOP LOSS TRIGGERED, TRADING HALTED for loss of %: " +
                            calculatePercentChange(lastEntryPrice, exit.getPrice()) + " on index: " + exit.getIndex()
                            + " (price=" + exit.getPrice().toDouble()
                            + ", amount=" + exit.getAmount().toDouble() + ")");*/
                    return true;
                }
            }
        }
        return false;
    }

    public TradingRecord getTradingRecord() {
        return tradingRecord;
    }

    public String getCurrencyPair() {
        return  currencyPair;
    }

    public Tick getCurrentTick() {
        final Tick tick = timeSeries.getTick(curIter);
        return tick;
    }

    public TimeSeries getTimeSeries() {
        return timeSeries;
    }

}
