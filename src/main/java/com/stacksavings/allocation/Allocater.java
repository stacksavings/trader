package com.stacksavings.allocation;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.TradingRecord;

public interface Allocater {

    public Decimal getAmounToSpend (final TradingRecord tradingRecord);


}
