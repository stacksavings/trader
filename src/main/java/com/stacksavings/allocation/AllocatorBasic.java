package com.stacksavings.allocation;

import com.stacksavings.Parameter.Parameters;
import com.stacksavings.tradingrecord.holders.TradingRecordHolder;
import com.stacksavings.utils.GenericUtils;
import eu.verdelhan.ta4j.*;

import java.util.List;
import java.util.Map;

public class AllocatorBasic extends Allocator {

    public AllocatorBasic(final Parameters parameters) {
        super(parameters);
    }


    public void processTickBuys(final List<TradingRecordHolder> tradingRecordHolders, final Map<Integer, Integer> activePositionsAtIndexTracker, final int curIndex) {

        for (final TradingRecordHolder tradingRecordHolder : tradingRecordHolders) {

            final String currencyPair = tradingRecordHolder.getCurrencyPair();

            //have to skip the conversion currency
            if (currencyPair.equalsIgnoreCase(parameters.getConversionCurrency())) {
                continue;
            }

            /*
            final TradingRecord tradingRecord = tradingRecordHolder.getTradingRecord();
            final Tick tick = tradingRecordHolder.getCurrentTick();

            Decimal numberToBuy = determineTradeAmount(tradingRecord, tick.getClosePrice());

            final Decimal totalWantedToBuy= tick.getClosePrice().multipliedBy(numberToBuy);

            final Decimal availableFundsForTrade = getFundsForTrade(totalWantedToBuy, curIndex);

            numberToBuy = availableFundsForTrade.dividedBy(tick.getClosePrice());

            if (numberToBuy.isGreaterThan(Decimal.ZERO)) {
                boolean entered = tradingRecordHolder.enterTrade(tick.getClosePrice(), numberToBuy);

                if (entered) {
                    Order order = tradingRecord.getLastEntry();
                    loggerHelper.logTickRow(currencyPair,"ENTER", order.getIndex(), order.getPrice().toDouble(), order.getAmount().toDouble());

                    //TODO this needs to be re-worked
                    //AutomatedTrader.updateActivePositionsAtIndex(tradingRecord, activePositionsAtIndexTracker, curIndex, parameters);
                }
            }*/

            //TODO this needs to be the allocator basic strategy, the one above should be a differnet implementation as it is more complex and error prone
            final TradingRecord tradingRecord = tradingRecordHolder.getTradingRecord();

            Decimal availableAmount = Decimal.HUNDRED;
            if (tradingRecord.getLastExit() != null) {
                availableAmount = tradingRecord.getLastExit().getAmount().multipliedBy(tradingRecord.getLastExit().getPrice());
            }
            availableAmount = applyFee(availableAmount);

            final Tick tick = tradingRecordHolder.getCurrentTick();
            final Decimal numberToBuy = availableAmount.dividedBy(tick.getClosePrice());
            boolean entered = tradingRecordHolder.enterTrade(tick.getClosePrice(), numberToBuy);
            if (entered) {
                Order order = tradingRecord.getLastEntry();
                loggerHelper.logTickRow(currencyPair,"ENTER", order.getIndex(), order.getPrice().toDouble(), order.getAmount().toDouble());
            }
        }
    }

    /**
     * Get the actual amt needed in BTC for the trade, converting from the conversion curency if needed (and applying fees)
     * @param totalWanted
     * @return return true if the full amount needed is now available in BTC
     */
    protected Decimal getFundsForTrade(final Decimal totalWanted, final int iter) {

        Decimal retAmt = Decimal.ZERO;

        //first see if we have this amount available in BTC
        final Decimal btcAmt = withdrawBtc(totalWanted, iter);

        final Decimal convertedBtcAmt = GenericUtils.convertfromBtc(btcAmt, iter, conversionSeries);

        retAmt = convertedBtcAmt;

        //if there wasn't enough BTC, now try to get funds from the
        if (retAmt.isLessThan(totalWanted)) {
            Decimal remainingAmtNeeded = totalWanted.minus(retAmt);
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
     * @param tradingRecordHolders
     * @param iter
     */
    public void processAccountingForSales(final List<TradingRecordHolder> tradingRecordHolders, final int iter) {

        Decimal totalConversionCur = Decimal.ZERO;

        for (final TradingRecordHolder tradingRecordHolder : tradingRecordHolders) {
            final Trade trade = tradingRecordHolder.getTradingRecord().getLastTrade();
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
