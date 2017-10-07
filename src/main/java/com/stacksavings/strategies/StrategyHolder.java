package com.stacksavings.strategies;

import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TradingRecord;

/**
 * Wrapper to build strategies and also for executing them to allow a way to add in logging, etc, if needed.
 */
public abstract class StrategyHolder {

    protected TimeSeries series;
    protected int shortTimeFrame;
    protected int longTimeFrame;
    protected Strategy strategy;


    public StrategyHolder(final int shortTimeFrame, final int longTimeFrame) {
        this.shortTimeFrame = shortTimeFrame;
        this.longTimeFrame = longTimeFrame;
    }

    public boolean shouldEnter(final int curIndex, final TradingRecord tradingRecord) {
        return strategy.shouldEnter(curIndex, tradingRecord);
    }

    public boolean shouldExit(final int curIndex, final TradingRecord tradingRecord) {
        return strategy.shouldExit(curIndex, tradingRecord);
    }


    /**
     * Should be called for each new time series, will clear out the previous strategy
     * @param series
     */
    public void setup(final TimeSeries series) {
        strategy = null;
        strategy = buildStrategy(series);

    }

    public abstract Strategy buildStrategy(final TimeSeries series);

    public abstract String getStrategyName();



}
