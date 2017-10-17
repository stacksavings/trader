package com.stacksavings.allocation;

import com.stacksavings.Parameter.Parameters;
import com.stacksavings.tradingrecord.holders.TradingRecordHolder;
import com.stacksavings.utils.GenericUtils;
import com.stacksavings.utils.LoggerHelper;
import eu.verdelhan.ta4j.*;

import java.util.List;
import java.util.Map;

public abstract class Allocator {

    protected Parameters parameters;
    protected LoggerHelper loggerHelper;
    protected TimeSeries conversionSeries;

    private  Decimal btcBalance;
    private  Decimal conversionCurrencyBalance;

    public Allocator(final Parameters parameters) {

        this.parameters = parameters;
        btcBalance = Decimal.ZERO;
        conversionCurrencyBalance = Decimal.ZERO;

    }

    public void init(final LoggerHelper loggerHelper, final TimeSeries conversionSeries, final List<String> currencies) {
        this.loggerHelper = loggerHelper;
        this.conversionSeries = conversionSeries;

        conversionCurrencyBalance = parameters.getInitialCurrencyAmount().multipliedBy(Decimal.valueOf(currencies.size()));

    }

    public Decimal getBtcBalance() {
        return btcBalance;
    }


    protected Decimal withdrawAllBtc() {
        final Decimal withdrawAmt = btcBalance;
        btcBalance = Decimal.ZERO;
        return withdrawAmt;
    }

    protected Decimal withdrawBtc(final Decimal conversionCurAmt, final int iter) {

        final Decimal btcAmountNeeded = GenericUtils.convertToBtc(conversionCurAmt, iter, conversionSeries);

        final Decimal resultingBtcBalance = btcBalance.minus(btcAmountNeeded);
        if (!resultingBtcBalance.isNegative()) {
            btcBalance = resultingBtcBalance;
            return btcAmountNeeded;
        }
        Decimal partialReturnAmount = btcBalance;
        btcBalance = Decimal.ZERO;
        return partialReturnAmount;

    }

    protected void depositBtc(final Decimal conversionCurAmt, final int iter) {
        final Decimal btcAmountToAdd = GenericUtils.convertToBtc(conversionCurAmt, iter, conversionSeries);
        btcBalance = btcAmountToAdd.plus(btcBalance);
    }

    public Decimal getConversionCurrencyBalance() {
        return conversionCurrencyBalance;
    }

    //TODO this probably shouldn't be used in live trading, instead would need new methods that take in btc
    protected void depositConversionCurrency(final Decimal conversionCurAmt) {
        final Decimal afterFeeAmt = applyFee(conversionCurAmt);
        conversionCurrencyBalance = afterFeeAmt.plus(conversionCurrencyBalance);
    }

    //TODO this can't be used as-is, needs to apply fee
    protected Decimal withdrawAllConversionCurrency() {
        final Decimal withdrawAmt = conversionCurrencyBalance;
        conversionCurrencyBalance = Decimal.ZERO;
        return withdrawAmt;
    }

    //TODO this probably shouldn't be used in live trading, instead would need new methods that take in btc
    protected Decimal withdrawConversionCurrency(final Decimal conversionCurAmt) {
        final Decimal afterFeeAmt = applyFee(conversionCurAmt);
        final Decimal resultingConversionCurBalance = conversionCurrencyBalance.minus(conversionCurAmt);
        if (!resultingConversionCurBalance.isNegative()) {
            conversionCurrencyBalance = resultingConversionCurBalance;
            return afterFeeAmt;
        }
        Decimal partialReturnAmount = applyFee(conversionCurrencyBalance);
        conversionCurrencyBalance = Decimal.ZERO;
        return partialReturnAmount;
    }

    protected Decimal applyFee(final Decimal conversionCurAmt) {
        final Decimal resultingAmt = conversionCurAmt.minus(conversionCurAmt.multipliedBy(parameters.getFeeAmount()));
        return resultingAmt;
    }


    public abstract void processTickBuys(final List<TradingRecordHolder> tradingRecordHolders, final Map<Integer, Integer> activePositionsAtIndexTracker, final int curIndex);

    protected abstract Decimal determineTradeAmount(final TradingRecord tradingRecord, final Decimal currentPrice);

    public abstract void processAccountingForSales(final List<TradingRecordHolder> tradingRecordHolders, final int iter);

    public abstract void iterationFinalAccountingProcessing(final int iter);


}
