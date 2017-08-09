package com.stacksavings.indicators;

import java.util.ArrayList;
import java.util.List;

import com.stacksavings.client.api.PoloniexClientApi;
import com.stacksavings.loaders.CsvTicksLoader;
import com.stacksavings.utils.Constants;
import com.stacksavings.utils.FileManager;
import com.stacksavings.utils.ROCIndicatorUtils;

import eu.verdelhan.ta4j.Decimal;
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
	
	/**
	 * 
	 * @return
	 */
	public static ROCIndicatorCalculate getInstance() 
	{
	      if(instance == null) 
	      {
	         instance = new ROCIndicatorCalculate();
	      }
	      
	      return instance;
	}	
	
	/**
	 * 
	 */
	private ROCIndicatorCalculate()
	{
		csvTicksLoader = CsvTicksLoader.getInstance();
		poloniexClientApi = PoloniexClientApi.getInstance();
		fileManager = FileManager.getInstance();
	}
	
	
	/**
	 * calculateROC
	 */
	public void calculateROC()
	{
		List<String> currencyPairList = poloniexClientApi.returnCurrencyPair();
		for (String currency : currencyPairList) 
		{
			String fileNameCurrencyPair = fileManager.getFileNameByCurrency(currency);
			
			TimeSeries series = csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair);
			ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
			
			ROCIndicator roc = new ROCIndicator(closePrice, Constants.TIME_FRAME_12);
			
			final int nbTicks = series.getTickCount();
	        
	        List<Decimal> results =new ArrayList<Decimal>();
	        
	        for (int i = 0; i < nbTicks; i++) 
	        {
	        	results.add(roc.getValue(i)) ;
	        }
	        
	        List<Decimal> resultFinal =  ROCIndicatorUtils.calculateRisePrice(results);
	        System.out.println("****  BUY Signal for currency : "+currency);
	        for (Decimal decimal : resultFinal) 
	        {
				System.out.println(decimal);
			}
	        
		}
	}
}
