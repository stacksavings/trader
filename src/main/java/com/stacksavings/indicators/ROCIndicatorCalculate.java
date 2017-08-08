package com.stacksavings.indicators;

import java.util.List;

import com.stacksavings.client.api.PoloniexClientApi;
import com.stacksavings.loaders.CsvTicksLoader;
import com.stacksavings.utils.FileManager;

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
	private PoloniexClientApi poloniexClientApi;
	private FileManager fileManager;
	
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
		poloniexClientApi = PoloniexClientApi.getInstance();
		fileManager = FileManager.getInstance();
	}
	
	
	public void calculateROC()
	{
		List<String> currencyPairList = poloniexClientApi.returnCurrencyPair();
		for (String currency : currencyPairList) 
		{
			String fileNameCurrencyPair = fileManager.getFileNameByCurrency(currency);
			TimeSeries series = csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair);
			ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
			
			ROCIndicator roc = new ROCIndicator(closePrice, 12);

		}
	}
}
