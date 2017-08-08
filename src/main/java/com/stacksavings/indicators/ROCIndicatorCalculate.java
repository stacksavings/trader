package com.stacksavings.indicators;

import com.stacksavings.loaders.CsvTicksLoader;

import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.ROCIndicator;

/**
 * 
 * @author jpcol
 *
 */
public class ROCIndicatorCalculate {

	private static ROCIndicatorCalculate instance;
	private CsvTicksLoader csvTicksLoader;
	
	public static ROCIndicatorCalculate getInstance() 
	{
	      if(instance == null) 
	      {
	         instance = new ROCIndicatorCalculate();
	      }
	      
	      return instance;
	}	
	
	private ROCIndicatorCalculate()
	{
		csvTicksLoader = CsvTicksLoader.getInstance();
	}
	
	
	public void calculateROC()
	{
		TimeSeries series = csvTicksLoader.loadSeriesByFileName("fileName");
		ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
		
		ROCIndicator roc = new ROCIndicator(closePrice, 12);
	}
}
