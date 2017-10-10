package com.stacksavings.allocation;

import com.stacksavings.Parameter.Parameters;
import com.stacksavings.indicators.AutomatedTrader;
import com.stacksavings.utils.LoggerHelper;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Order;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TradingRecord;

import java.util.Map;

public class AllocatorBasic extends Allocator {

    public AllocatorBasic(final Parameters parameters) {
        super(parameters);
    }


    public void processTickBuys(final Map<String, Tick> buyTicks, final Map<String, TradingRecord> buyTradingRecords, final Map<Integer, Integer> activePositionsAtIndexTracker, final int curIndex) {


        for (final String currencyPair : buyTicks.keySet()) {

            final TradingRecord tradingRecord = buyTradingRecords.get(currencyPair);
            final Tick tick = buyTicks.get(currencyPair);

            Decimal numberToBuy = determineTradeAmount(tradingRecord, tick.getClosePrice());

            boolean entered = AutomatedTrader.enterTrade(currencyPair, tradingRecord, tick.getClosePrice(), curIndex, numberToBuy, parameters);

            if (entered) {
                Order order = tradingRecord.getLastEntry();
                loggerHelper.logTickRow(currencyPair,"ENTER", order.getIndex(), order.getPrice().toDouble(), order.getAmount().toDouble());
                AutomatedTrader.updateActivePositionsAtIndex(tradingRecord, activePositionsAtIndexTracker, curIndex, parameters);
            }

        }
    }

    //TODO this seems to need to use the starting funds variable, hard-coding this to use 'initialSpendAmtPerCurrency' is confusing and probably not optimal, this variable is deprecated
    protected Decimal determineTradeAmount(final TradingRecord tradingRecord, final Decimal currentPrice) {
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




}
