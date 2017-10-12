package com.stacksavings.allocation;

import com.stacksavings.Parameter.Parameters;
import com.stacksavings.indicators.AutomatedTrader;
import com.stacksavings.utils.GenericUtils;
import com.stacksavings.utils.LoggerHelper;
import eu.verdelhan.ta4j.*;

import java.util.List;
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

            final Decimal totalNeeded = tick.getClosePrice().multipliedBy(numberToBuy);

            final Decimal availableFundsForTrade = getFundsForTrade(totalNeeded, curIndex);
            numberToBuy = determineTradeAmount(tradingRecord, availableFundsForTrade);

            if (!availableFundsForTrade.isNegative()) {
                boolean entered = AutomatedTrader.enterTrade(currencyPair, tradingRecord, availableFundsForTrade, curIndex, numberToBuy, parameters);

                if (entered) {
                    Order order = tradingRecord.getLastEntry();
                    loggerHelper.logTickRow(currencyPair,"ENTER", order.getIndex(), order.getPrice().toDouble(), order.getAmount().toDouble());
                    //AutomatedTrader.updateActivePositionsAtIndex(tradingRecord, activePositionsAtIndexTracker, curIndex, parameters);
                }
            }
        }
    }

    /**
     * Get the actual amt needed in BTC for the trade, converting from the conversion curency if needed (and applying fees)
     * @param totalNeeded
     * @return return true if the full amount needed is now available in BTC
     */
    protected Decimal getFundsForTrade(final Decimal totalNeeded, final int iter) {

        Decimal retAmt = Decimal.ZERO;

        //first see if we have this amount available in BTC
        final Decimal btcAmt = withdrawBtc(totalNeeded, iter);

        final Decimal convertedBtcAmt = GenericUtils.convertfromBtc(btcAmt, iter, conversionSeries);

        retAmt = convertedBtcAmt;

        //if there wasn't enough BTC, now try to get funds from the
        if (retAmt.isLessThan(totalNeeded)) {
            Decimal remainingAmtNeeded = totalNeeded.minus(retAmt);
            retAmt = retAmt.plus(withdrawConversionCurrency(remainingAmtNeeded));
        }

        return retAmt;
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

    /**
     * Deposit any BTC from this iteration's sales into our tracking object
     * @param trades
     * @param iter
     */
    public void processAccountingForSales(final List<Trade> trades, final int iter) {

        Decimal totalConversionCur = Decimal.ZERO;

        for (final Trade trade : trades) {
            final Decimal tradeTotal = trade.getEntry().getAmount().multipliedBy(trade.getEntry().getPrice());
            totalConversionCur = totalConversionCur.plus(tradeTotal);
        }

        depositBtc(totalConversionCur, iter);

    }

    /**
     * Final accounting step to process any remaining BTC funds for an iteration
     */
    public void iterationFinalAccountingProcessing(final int iter) {

        if (!getBtcBalance().isNegative()) {
            final Decimal withdrawnBtc = withdrawAllBtc();
            final Decimal conversionCurAmt = GenericUtils.convertfromBtc(withdrawnBtc, iter, conversionSeries);

            depositConversionCurrency(conversionCurAmt);
        }
    }

}
