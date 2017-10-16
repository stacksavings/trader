package com.stacksavings.tradingrecord.holders;

import com.stacksavings.loaders.CsvTicksLoader;
import com.stacksavings.strategies.StrategyHolder;
import com.stacksavings.utils.FileManager;
import com.stacksavings.utils.GenericUtils;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.TimeSeries;

import java.util.List;
import java.util.Map;

public class TradingRecordHolderFactory {

    public static TradingRecordHolder createTradingRecordHolder(final String currencyPair, final Decimal stopLossRatio, final Decimal feeAmount, final String fromDate, final String toDate,
                                                                final StrategyHolder strategyHolder, final TimeSeries conversionTimeSeries, final boolean isLiveTradingMode, final boolean useBuySellCache) {

        TradingRecordHolder tradingRecordHolder = null;
        if (isLiveTradingMode) {
            tradingRecordHolder = new TradingRecordHolderLiveTrading();

        } else if (useBuySellCache) {

            List<Map<String, Boolean>> buySellCache = GenericUtils.loadBuySellCache( currencyPair, fromDate, toDate, FileManager.getInstance(), CsvTicksLoader.getInstance());

            TradingRecordHolderCacheEnabled tradingRecordHolderCacheEnabled = new TradingRecordHolderCacheEnabled();
            tradingRecordHolderCacheEnabled.setBuySellCache(buySellCache);

            tradingRecordHolder = tradingRecordHolderCacheEnabled;

        } else {
            tradingRecordHolder = new TradingRecordHolder();
        }

        tradingRecordHolder.init(currencyPair, conversionTimeSeries, stopLossRatio, fromDate, toDate, strategyHolder, feeAmount);

        return tradingRecordHolder;

    }


}
