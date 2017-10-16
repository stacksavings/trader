package com.stacksavings.tradingrecord;

import com.stacksavings.loaders.CsvTicksLoader;
import com.stacksavings.strategies.StrategyHolder;
import com.stacksavings.tradingrecord.holders.TradingRecordHolder;
import com.stacksavings.utils.FileManager;
import com.stacksavings.utils.GenericUtils;
import com.stacksavings.utils.LoggerHelper;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TradingRecord;
import org.jfree.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TradingRecordCollection {

    private int curIter;
    private Map<String, TradingRecordHolder> tradingRecordHolderMap;

    private LoggerHelper loggerHelper;

    private String conversionCurrency;

    //-> push up
    private boolean useConversionSeries;

    private Map<String, TradingRecordHolder> curIterSellTradingRecordHolders;
    private Map<String, TradingRecordHolder> curIterBuyTradingRecordHolders;

    private Map<Integer, Integer> activePositionsAtIndexTracker;

    private TimeSeries conversionTimeSeries;

    private Map<String, List<Map<String, Boolean>>> buySellCache = new HashMap<String, List<Map<String, Boolean>>>();

    private Map<String, TradingRecord> backTestTradingRecords;

    private int totalIterations;

    private List<String> currencyPairList;

    private StrategyHolder strategyHolder;

    private CsvTicksLoader csvTicksLoader;
    private FileManager fileManager;

    public TradingRecordCollection() {

    }

    public int getTotalIterations() {
        return  totalIterations;
    }

    public void init(final String conversionCurrency, final List<String> currencyPairList, final StrategyHolder strategyHolder, final String fromDate, final String toDate) {

        this.strategyHolder = strategyHolder;
        this.currencyPairList = currencyPairList;
        this.conversionCurrency = conversionCurrency;

        this.curIter = 0;

        tradingRecordHolderMap = new HashMap<String, TradingRecordHolder>();

        fileManager = FileManager.getInstance();
        csvTicksLoader = CsvTicksLoader.getInstance();

        loggerHelper = LoggerHelper.getInstance();

        conversionTimeSeries = GenericUtils.loadTimeSeries(this.conversionCurrency, fromDate, toDate, false, null, fileManager, csvTicksLoader);

        backTestTradingRecords = new HashMap<String, TradingRecord>();

        //   if (!parameters.isLiveTradeMode() && parameters.isUseCachedBuySellSignals()) {
        buySellCache = new HashMap<String, List<Map<String, Boolean>>>();
        for (final String currency : currencyPairList) {
            buySellCache.put(currency, new ArrayList<Map<String, Boolean>>());
        }
        //    }

        activePositionsAtIndexTracker = new HashMap<Integer, Integer>();

        totalIterations = 1;
      //  if (!parameters.isLiveTradeMode()) {
            totalIterations = conversionTimeSeries.getTickCount();
     //   }
    }


    public void processIteration(final int curIter) {
        this.curIter = curIter;

        this.curIterBuyTradingRecordHolders = new HashMap<String, TradingRecordHolder> ();
        this.curIterSellTradingRecordHolders = new HashMap<String, TradingRecordHolder> ();

        processIterationExits();
        processIterationEnters();

    }

    private TradingRecordHolder processIterSetup(final String currencyPair) {

        final TradingRecordHolder tradingRecordHolder = tradingRecordHolderMap.get(currencyPair);

        final TimeSeries series = tradingRecordHolder.getTimeSeries();

        //TODO this doesn't seem good, not properly object oriented, consider re-working
        strategyHolder.setup(series);

        return tradingRecordHolder;
    }

    private void processIterationExits() {

        for (String currencyPair : currencyPairList) {
            if (skipCurrencyNonTradingReason(currencyPair)) {
                continue;
            }

            try {

                final TradingRecordHolder tradingRecordHolder = processIterSetup(currencyPair);

                final boolean exitIndicated = tradingRecordHolder.processTickExit(curIter);
                if (exitIndicated) {
                    curIterSellTradingRecordHolders.put(currencyPair, tradingRecordHolder);
                }

            } catch (final Exception e) {
                loggerHelper.getDefaultLogger().error("Exception encountered for currency " + currencyPair + ", stack trace follows: ", e);
            }
        }

    }

    private void processIterationEnters() {
        for (String currencyPair : currencyPairList) {
            if (skipCurrencyNonTradingReason(currencyPair)) {
                continue;
            }

            try {

                final TradingRecordHolder tradingRecordHolder = processIterSetup(currencyPair);

                final boolean enterIndicated = tradingRecordHolder.processEnterStrategy(curIter);

                if (enterIndicated) {
                    curIterBuyTradingRecordHolders.put(currencyPair, tradingRecordHolder);
                }
/*                else {
                    //TODO this needs re-working
                    //This tick / trading record pair is finished so record the active position, if applicable
                    //updateActivePositionsAtIndex(tradingRecord, activePositionsAtIndexTracker, curIter, parameters);
                }*/


            } catch (final Exception e) {
                loggerHelper.getDefaultLogger().error("Exception encountered for currency " + currencyPair + ", stack trace follows: ", e);
            }
        }
    }


    /**
     * Skip a currency for a reason other than that it is determined to not be optimal to trade. An example is that we are basing buy / sell off of usdt so we can't also trade it. This differs
     * from currencies that we skip due to having determined that they may not be optimal to trade from backtesting, for example.
     * @param currency
     * @return
     */
    private boolean skipCurrencyNonTradingReason(final String currency) {
        if (StringUtils.startsWithIgnoreCase(currency, conversionCurrency)) {
            return true;
        }
        return false;
    }

    public TradingRecordHolder getTradingRecordHolder(final String currencyPair) {
        return tradingRecordHolderMap.get(currencyPair);
    }

    public TimeSeries getConversionTimeSeries() {
        return conversionTimeSeries;
    }

    public List<TradingRecordHolder> getCurIterSellTradingRecordHolders() {
        return new ArrayList<TradingRecordHolder>(curIterSellTradingRecordHolders.values());
    }

    public List<TradingRecordHolder> getCurIterBuyTradingRecordHolders() {
        return new ArrayList<TradingRecordHolder>(curIterBuyTradingRecordHolders.values());
    }

    public void addTradingRecordHolder(final String currencyPair, final TradingRecordHolder tradingRecordHolder) {
        tradingRecordHolderMap.put(currencyPair, tradingRecordHolder);
    }

}
