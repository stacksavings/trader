package com.stacksavings.strategies;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Rule;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.oscillators.StochasticOscillatorKIndicator;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.EMAIndicator;
import eu.verdelhan.ta4j.indicators.trackers.MACDIndicator;
import eu.verdelhan.ta4j.indicators.trackers.SMAIndicator;
import eu.verdelhan.ta4j.trading.rules.*;


/**
 * 
 * @author jpcol
 *
 */
public class BuySellStrategy {

	/**
	 * SMA
	 * @param series
	 * @return
	 */
	public static Strategy buildStrategy(TimeSeries series) {
		
        if (series == null) {
            throw new IllegalArgumentException("Series cannot be null");
        }
        
        // Getting the close price of the ticks
        Decimal firstClosePrice = series.getTick(0).getClosePrice();
        System.out.println("First close price: " + firstClosePrice.toDouble());
        // Or within an indicator:
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        // Here is the same close price:
        System.out.println(firstClosePrice.isEqual(closePrice.getValue(0))); // equal to firstClosePrice

        // Getting the simple moving average (SMA) of the close price over the last 5 ticks
        SMAIndicator shortSma = new SMAIndicator(closePrice, 5);
        // Here is the 5-ticks-SMA value at the 42nd index
        System.out.println("5-ticks-SMA value at the 42nd index: " + shortSma.getValue(42).toDouble());

        // Getting a longer SMA (e.g. over the 30 last ticks)
        SMAIndicator longSma = new SMAIndicator(closePrice, 30);


        // Ok, now let's building our trading rules!

        // Buying rules
        // We want to buy:
        //  - if the 5-ticks SMA crosses over 30-ticks SMA
        //  - or if the price goes below a defined price (e.g $0.83)
        Rule buyingRule = new CrossedUpIndicatorRule(shortSma, longSma);
                //.or(new CrossedDownIndicatorRule(closePrice, Decimal.valueOf("0.819")));
        
        // Selling rules
        // We want to sell:
        //  - if the 5-ticks SMA crosses under 30-ticks SMA
        //  - or if if the price looses more than 3%
        //  - or if the price earns more than 2%
        Rule sellingRule = new CrossedDownIndicatorRule(shortSma, longSma)
                .or(new StopLossRule(closePrice, Decimal.valueOf("3")))
                .or(new StopGainRule(closePrice, Decimal.valueOf("2")));

        return new Strategy(buyingRule, sellingRule);
	}

	/**
	 * Build Exponential Moving Average strategy
	 * @param series
	 * @param shortTimeFrame
	 * @param longTimeFrame
	 * @return
	 */
	public static Strategy buildStrategyEMA(TimeSeries series, final int shortTimeFrame, final int longTimeFrame) {
		 if (series == null) {
	            throw new IllegalArgumentException("Series cannot be null");
	        }

	        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

	        // The bias is bullish when the shorter-moving average moves above the longer moving average.
	        // The bias is bearish when the shorter-moving average moves below the longer moving average.
	        EMAIndicator shortEma = new EMAIndicator(closePrice, shortTimeFrame);
	        EMAIndicator longEma = new EMAIndicator(closePrice, longTimeFrame);

	        StochasticOscillatorKIndicator stochasticOscillK = new StochasticOscillatorKIndicator(series, 14);

	        MACDIndicator macd = new MACDIndicator(closePrice, shortTimeFrame, longTimeFrame);
	        EMAIndicator emaMacd = new EMAIndicator(macd, 18);

	        // Entry rule
	        Rule entryRule = new OverIndicatorRule(shortEma, longEma) // Trend
	                .and(new CrossedDownIndicatorRule(stochasticOscillK, Decimal.valueOf(20))) // Signal 1
	                .and(new OverIndicatorRule(macd, emaMacd))
					;


	        // Exit rule
	        Rule exitRule = new UnderIndicatorRule(shortEma, longEma) // Trend
	                .and(new CrossedUpIndicatorRule(stochasticOscillK, Decimal.valueOf(80))) // Signal 1
	                .and(new UnderIndicatorRule(macd, emaMacd))
					;


	        return new Strategy(entryRule, exitRule);
	}



}
