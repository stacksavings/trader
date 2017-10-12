package com.stacksavings.utils;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;

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

}
