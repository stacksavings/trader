package com.stacksavings.utils;

import com.stacksavings.Parameter.Parameters;
import com.stacksavings.loaders.CsvTicksLoader;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A holder for any util methods, later these should probably be re-factored to more specific util classes to keep things more organized
 */
public class GenericUtils {


    public static Decimal convertToBtc(final Decimal amount, final int iter,final TimeSeries conversionSeries) {

        final Tick conversionTick = conversionSeries.getTick(iter);
        final Decimal convertedAmt = amount.dividedBy(conversionTick.getClosePrice());
        return convertedAmt;
    }

    public static Decimal convertfromBtc(final Decimal amount, final int iter,final TimeSeries conversionSeries) {

        final Tick conversionTick = conversionSeries.getTick(iter);
        final Decimal convertedAmt = amount.multipliedBy(conversionTick.getClosePrice());
        return convertedAmt;
    }

    public static Decimal applyBuyFee (final Decimal price, final Decimal feeAmount) {
        final Decimal buyPrice = (price.multipliedBy(feeAmount)).plus(price);
        return buyPrice;
    }

    public static Decimal applySellFee (final Decimal price, final Decimal feeAmount) {
        final Decimal sellPrice = price.minus((price.multipliedBy(feeAmount)));
        return sellPrice;
    }

    public static TimeSeries loadTimeSeries(final String currencyPair, final boolean useConversionSeries, final TimeSeries conversionTimeSeries, final FileManager fileManager, final CsvTicksLoader csvTicksLoader) {
        String fileNameCurrencyPair = null;
        TimeSeries timeSeries = null;

        fileNameCurrencyPair = fileManager.getFileNameByCurrency(currencyPair);

        timeSeries = csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair, useConversionSeries, conversionTimeSeries, null);

        return timeSeries;
    }

    public static TimeSeries loadTimeSeries(final String currencyPair, final String fromDate, final String toDate, final boolean useConversionSeries, final TimeSeries conversionTimeSeries, final FileManager fileManager, final CsvTicksLoader csvTicksLoader) {
        String fileNameCurrencyPair = null;
        TimeSeries timeSeries = null;

        final File currencyPairFile = fileManager.getFileByName(fromDate, toDate, currencyPair);
        fileNameCurrencyPair = currencyPairFile.getAbsolutePath();

        timeSeries = csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair, useConversionSeries, conversionTimeSeries, null);

        return timeSeries;
    }

    //TODO this should be refactored to be part of a type that wraps TimeSeries
    public static List<Map<String, Boolean>> loadBuySellCache(final String currencyPair, final String fromDate, final String toDate, final FileManager fileManager, final CsvTicksLoader csvTicksLoader) {

        final List<Map<String, Boolean>> buySellCacheForCurrency = new ArrayList<Map<String, Boolean>>();

        final File currencyPairFile = fileManager.getFileByName(fromDate, toDate, currencyPair);
        final String fileNameCurrencyPair = currencyPairFile.getAbsolutePath();

        csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair, false, null, buySellCacheForCurrency);

        return  buySellCacheForCurrency;
    }

    public static List<String> filterCurrencyList(final List<String> currencyPairList, final List<String> currencyIncludeList , final List<String> currencySkipList) {
        final List<String> currencyPairListRet = new ArrayList<String>();
        for (final String currencyPair : currencyPairList) {
            if ( (currencySkipList != null && currencySkipList.contains(currencyPair))
                    || (currencyIncludeList != null && !currencyIncludeList.contains(currencyPair))
                    ) {
                continue;
            }
            currencyPairListRet.add(currencyPair);
        }
        return  currencyPairListRet;
    }

}
