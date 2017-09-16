package com.stacksavings.indicators;

import java.util.ArrayList;
import java.util.List;

import com.stacksavings.client.api.PoloniexClientApi;
import com.stacksavings.client.api.dto.ROCIndicatorDto;
import com.stacksavings.loaders.CsvTicksLoader;
import com.stacksavings.strategies.BuySellStrategy;
import com.stacksavings.utils.Constants;
import com.stacksavings.utils.FileManager;
import com.stacksavings.utils.PropertiesUtil;
import com.stacksavings.utils.ROCIndicatorUtils;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.TradingRecord;
import eu.verdelhan.ta4j.Trade;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.ROCIndicator;
import org.joda.time.DateTime;

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
	private PropertiesUtil propertiesUtil;
	private int numResultsPerCurrency;
	
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
		propertiesUtil = PropertiesUtil.getInstance();
		numResultsPerCurrency = Integer.parseInt(propertiesUtil.getProps().getProperty("num_results_per_currency"));
	}
	
	
	/**
	 * calculateROC
	 */
	public void calculateROC()
	{
		List<String> currencyPairList = poloniexClientApi.returnCurrencyPair();
		
		if(currencyPairList != null && currencyPairList.size() > 0)
		{
			for (String currency : currencyPairList) 
			{
				String fileNameCurrencyPair = fileManager.getFileNameByCurrency(currency);
				
				final TimeSeries series = csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair);
				final ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

				//TODO We should consider pulling the time frame values from a config file
				final Strategy strategy = BuySellStrategy.buildStrategyEMA(series, 9, 26);
				final TradingRecord tradingRecord = series.run(strategy);

				final List<Trade> trades = tradingRecord.getTrades();
				for (final Trade trade : trades) {
					printTradeHelper(trade, currency, series);
				}



				//This code may need to be re-worked, or moved, as it is not in the format of a strategy
				boolean runROCIndicatorCode = false;
				if (runROCIndicatorCode) {
					ROCIndicator roc = new ROCIndicator(closePrice, Constants.TIME_FRAME_12);

					final int nbTicks = series.getTickCount();

					List<Decimal> results =new ArrayList<Decimal>();
					List<ROCIndicatorDto> resultROC = new ArrayList<ROCIndicatorDto>();

					for (int i = 0; i < nbTicks; i++)
					{
						results.add(roc.getValue(i));

						resultROC.add(new ROCIndicatorDto(series.getTick(i), roc.getValue(i)));
					}

					List<ROCIndicatorDto> resultFinal =  ROCIndicatorUtils.calculateRisePriceDto(resultROC, numResultsPerCurrency);
					System.out.println("****  BUY Signal for currency : "+currency);
					for (ROCIndicatorDto rocIndicatorDto : resultFinal)
					{
						System.out.println("BeginTime: "+rocIndicatorDto.getTick().getEndTime()+ " Decimal: "+rocIndicatorDto.getDecimal()+" Price: "+rocIndicatorDto.getTick().getClosePrice());
					}
				}
			}
		} 
		else
		{
			System.out.println("No hay datos del servicio web");
		}

	}

	public static void printTradeHelper(final Trade trade, final String currency, final TimeSeries series) {
		System.out.println("**** TRADE for " + currency);

		//TODO Not sure if getEndtime() is the right time that we want
		final DateTime enterTime = series.getTick(trade.getEntry().getIndex()).getEndTime();
		final DateTime exitTime = series.getTick(trade.getExit().getIndex()).getEndTime();

		final Decimal entryPrice = trade.getEntry().getPrice();
		final Decimal exitPrice = trade.getExit().getPrice();
		final Decimal percentChange = exitPrice.minus(entryPrice).dividedBy(entryPrice).multipliedBy(Decimal.valueOf(100));

		System.out.println();

		System.out.println("Enter: " + entryPrice + " Time: " + enterTime);
		System.out.println("Exit: " + exitPrice + " Time: " + exitTime);
		System.out.println("Gain %: " + percentChange);

		System.out.println();
	}
	
	public static void main(String[] args) {

		ROCIndicatorCalculate.getInstance().calculateROC();
		
	}
}
