package com.stacksavings.utils;

import com.stacksavings.Parameter.Parameters;
import com.stacksavings.loaders.CsvTicksLoader;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;

import java.io.File;

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

    public static TimeSeries loadTimeSeries(final String currency, final boolean useConversionSeries, final TimeSeries conversionTimeSeries, final FileManager fileManager, final CsvTicksLoader csvTicksLoader) {
        String fileNameCurrencyPair = null;
        TimeSeries timeSeries = null;

        fileNameCurrencyPair = fileManager.getFileNameByCurrency(currency);

        timeSeries = csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair, useConversionSeries, conversionTimeSeries, null);

        return timeSeries;
    }

    public static TimeSeries loadTimeSeries(final String currency, final String fromDate, final String toDate, final boolean useConversionSeries, final TimeSeries conversionTimeSeries, final FileManager fileManager, final CsvTicksLoader csvTicksLoader) {
        String fileNameCurrencyPair = null;
        TimeSeries timeSeries = null;

        final File currencyPairFile = fileManager.getFileByName(fromDate, toDate, currency);
        fileNameCurrencyPair = currencyPairFile.getAbsolutePath();

        timeSeries = csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair, useConversionSeries, conversionTimeSeries, null);

        return timeSeries;
    }

}
