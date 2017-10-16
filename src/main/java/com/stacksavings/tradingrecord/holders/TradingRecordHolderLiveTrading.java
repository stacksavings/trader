package com.stacksavings.tradingrecord.holders;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.TradingRecord;

import java.math.BigDecimal;

public class TradingRecordHolderLiveTrading extends TradingRecordHolder {

    @Override
    public boolean enterTrade(final Decimal closePrice, final Decimal numberToBuy) {

        final Decimal buyPriceDecimal = closePrice;

        final BigDecimal buyPrice = BigDecimal.valueOf(buyPriceDecimal.toDouble());

        //TODO need to refactor this for live trading mode
        //poloniexTraderClient.buy(currencyPair, buyPrice, BigDecimal.valueOf(numberToBuy.toDouble()), conversionTimeSeries);

        //TODO, figure what to do on this, we aren't necessarily going to know right away if a trade actually was processed for real time trading
        final boolean entered = enterTrade(closePrice, numberToBuy);

        return entered;
    }

    @Override
    protected boolean exitTrade(final String currencyPair, final TradingRecord tradingRecord, final Decimal closePrice,
                                final int curIndex, final Decimal numberToSell) {

        final Decimal sellPriceDecimal = closePrice;

        final BigDecimal sellPrice = BigDecimal.valueOf(sellPriceDecimal.toDouble());

        //TODO need to refactor this for live trading mode
        //poloniexTraderClient.sell(currencyPair, sellPrice, BigDecimal.valueOf(numberToSell.toDouble()), conversionTimeSeries);

        //TODO, figure what to do on this, we aren't necessarily going to know right away if a trade actually was processed for real time trading
        boolean exited = exitTrade(tradingRecord, closePrice, curIndex, numberToSell);

        return exited;

    }

}
