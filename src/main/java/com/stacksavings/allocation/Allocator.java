package com.stacksavings.allocation;

import com.stacksavings.Parameter.Parameters;
import com.stacksavings.utils.LoggerHelper;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TradingRecord;

import java.util.Map;

public abstract class Allocator {

    protected Parameters parameters;
    protected LoggerHelper loggerHelper;

    public Allocator(final Parameters parameters) {

        this.parameters = parameters;
    }

    public void init(final LoggerHelper loggerHelper) {
        this.loggerHelper = loggerHelper;
    }


    public abstract void processTickBuys(final Map<String, Tick> buyTicks, final Map<String, TradingRecord> buyTradingRecords, final int curIndex);

    protected abstract Decimal determineTradeAmount(final TradingRecord tradingRecord, final Decimal currentPrice);


}
