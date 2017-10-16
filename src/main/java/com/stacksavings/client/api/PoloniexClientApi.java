package com.stacksavings.client.api;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stacksavings.loaders.CsvTicksLoader;
import com.stacksavings.strategies.StrategyHolder;
import com.stacksavings.utils.*;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.TradingRecord;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stacksavings.client.api.dto.ChartData;

/**
 * 
 * @author jpcol <br>
 * This class is a singleton
 */
public class PoloniexClientApi {

	private static PoloniexClientApi instance;
	private PropertiesUtil propertiesUtil;
	private FileManager fileManager;
	private LoggerManager loggerManager;
	
	public static PoloniexClientApi getInstance() 
	{
	      if(instance == null) 
	      {
	         instance = new PoloniexClientApi();
	      }
	      
	      return instance;
	}	
	
	private PoloniexClientApi()
	{
		
		propertiesUtil = PropertiesUtil.getInstance();
		fileManager = FileManager.getInstance();
		loggerManager = LoggerManager.getInstance();
		
	}
	
	
	/**
	 * 
	 * Client API - To return a currency pair list
	 * @return
	 */
	public List<String> returnCurrencyPair(final String conversionCurrency)
	{
		loggerManager.info("begin returnCurrencyPair");
		
		int timeout = 5; // seconds
		final RequestConfig param = RequestConfig.custom().setConnectTimeout(timeout * 1000).build();
		CloseableHttpClient client = HttpClients.custom().setDefaultRequestConfig(param).build();
		
		String restApiService = propertiesUtil.getProps().getProperty("endpoint.api")+propertiesUtil.getProps().getProperty("return.ticker");
		
		List<String> currencyPair = new ArrayList<String>();
		try {
			
			HttpGet request = new HttpGet(restApiService);
			HttpResponse response;
			
			response = client.execute(request);
			HttpEntity entity1 = response.getEntity();
			JSONObject jsonObject =new JSONObject(EntityUtils.toString(entity1));
			Set<String> iterator = jsonObject.keySet();
			for (String item : iterator) {
				//Limit to just pairs starting with btc, for now, as this way we are always comparing same baseline currency
				if (StringUtils.startsWithIgnoreCase(item,"btc") || StringUtils.equalsIgnoreCase(item, conversionCurrency)) {
					currencyPair.add(item);
				}
			}
			
			loggerManager.info("end returnCurrencyPair");
			
			return currencyPair;

		} 
		catch (IOException e) 
		{
			loggerManager.error(e.getMessage());
		} 
		return null;
	}
	
	/**
	 * Client API - To return a chart data list
	 * @return
	 */
	public List<ChartData> returnChartData(String currencyPair) 
	{
		loggerManager.info("begin returnChartData:"+currencyPair);
		
		CloseableHttpClient client = HttpClients.createDefault();
		String restApiService = propertiesUtil.getProps().getProperty("endpoint.api")+propertiesUtil.getProps().getProperty("return.chart.data");
		
		try {
			
			SimpleDateFormat sdf =new SimpleDateFormat(Constants.YYYY_MM_DD_HH_MM_SS);
			
			String sDate = fileManager.getLastDateFromCSVFile(currencyPair);
			
			//ZonedDateTime zonedDateTime=ZonedDateTime.parse(sDate);
			
			Date dDate = sdf.parse(sDate);
			
			Date dDateNow = new Date();
						
			Long lDateBegin = dDate.getTime()/1000;
			//Long lDateBegin = zonedDateTime.toInstant().toEpochMilli()/1000;
						
			restApiService = restApiService.replaceAll("startbegin", lDateBegin.toString() );
			
			Long lDateEnd = dDateNow.getTime()/1000;
			//Long lDateEnd = ZonedDateTime.now().withZoneSameLocal(ZoneId.systemDefault()).toInstant().toEpochMilli()/1000;
			
			restApiService = restApiService.replaceAll("startend", lDateEnd.toString());
			
			restApiService = restApiService.replaceAll("currency_pair", currencyPair);
			
			HttpGet request = new HttpGet(restApiService);
			HttpResponse response;
			
			response = client.execute(request);
			HttpEntity entity1 = response.getEntity();
			byte[] byteData = EntityUtils.toByteArray(entity1);

			ObjectMapper objectMapper = new ObjectMapper();

			List<ChartData> chartDataList = Arrays.asList(objectMapper.readValue(byteData, ChartData[].class));

			loggerManager.info("end returnChartData");
			
			return chartDataList;

		} 
		catch (IOException e) 
		{
			loggerManager.error(e.getMessage());
		} 
		catch (ParseException e) 
		{
			loggerManager.error(e.getMessage());
		}
		
		return null;
	}

	
	/**
	 * 
	 * @param fromDate
	 * @param toDate
	 * @param currencyPair
	 * @return
	 */
	public List<ChartData> returnChartDataFromDateToDate(String fromDate, String toDate, String currencyPair)
	{
		loggerManager.info("begin returnChartData: "+currencyPair);

		currencyPair = currencyPair.toUpperCase();
		
		CloseableHttpClient client = HttpClients.createDefault();
		String restApiService = propertiesUtil.getProps().getProperty("endpoint.api")+propertiesUtil.getProps().getProperty("return.chart.data");

		try {
			
			SimpleDateFormat sdf =new SimpleDateFormat(Constants.YYYY_MM_DD_HH_MM_SS);
			
			Date dFromDate = sdf.parse(fromDate);
			
			Date dToDate = sdf.parse(toDate);
			
			Long lDateBegin = dFromDate.getTime()/1000;
			
			 Long lDateEnd = dToDate.getTime()/1000;
			// String sDate = fileManager.getLastDateFromCSVFile(currencyPair);
			
			// ZonedDateTime zonedDateTime=ZonedDateTime.parse(sDate);
			
			// Date dDate = sdf.parse(sDate);
			
			// Date dDateNow = new Date();
						
			// Long lDateBegin = dDate.getTime()/1000;
			// Long lDateBegin = zonedDateTime.toInstant().toEpochMilli()/1000;
						
			restApiService = restApiService.replaceAll("startbegin", lDateBegin.toString() );
			
			// Long lDateEnd = dDateNow.getTime()/1000;
			// Long lDateEnd = ZonedDateTime.now().withZoneSameLocal(ZoneId.systemDefault()).toInstant().toEpochMilli()/1000;
			
			restApiService = restApiService.replaceAll("startend", lDateEnd.toString() );
			
			restApiService = restApiService.replaceAll("currency_pair", currencyPair);

			HttpGet request = new HttpGet(restApiService);
			HttpResponse response;
			
			response = client.execute(request);
			HttpEntity entity1 = response.getEntity();

			byte[] byteData = EntityUtils.toByteArray(entity1);

			ObjectMapper objectMapper = new ObjectMapper();

			List<ChartData> chartDataList = Arrays.asList(objectMapper.readValue(byteData, ChartData[].class));

			loggerManager.info("end returnChartData");
			
			return chartDataList;

		} 
		catch (IOException e) 
		{
			loggerManager.error(e.getMessage());
		} 
		catch (ParseException e) 
		{
			loggerManager.error(e.getMessage());
		} 
			
		/**
		 catch (ParseException e) 
		{
			loggerManager.error(e.getMessage());
		}
		*/
		
		return null;
	}

	/**
	 * Run the strategy to record all entry / exit signals
	 * @param timeSeries
	 * @param chartDataList
	 * @param strategyHolder
	 */
	private void runStrategy(final String currencyPair, final TimeSeries timeSeries, final List<ChartData> chartDataList , final StrategyHolder strategyHolder) {

		strategyHolder.setup(timeSeries);
		for (int i = 0; i < chartDataList.size(); i++) {

			final ChartData chartData = chartDataList.get(i);

			final boolean shouldEnter = strategyHolder.shouldEnter(i, null);
			final boolean shouldExit = strategyHolder.shouldExit(i, null);

			//TODO - temporary debugging code
			if (currencyPair.equalsIgnoreCase("BTC_XEM") && i == 17) {
				LoggerHelper.logObject(timeSeries.getTick(i));

				LoggerHelper.logObject(timeSeries);
			}

			//TODO - temporary debugging code
			System.out.println("currency: " + currencyPair + " tick: " + i + " close price: " + timeSeries.getTick(i).getClosePrice() + " shouldEnter: " +shouldEnter+ " shouldExit: " +shouldExit);

			chartData.setStrategyShouldEnter(shouldEnter);
			chartData.setStrategyShouldExit(shouldExit);
		}
	}
	
	
	/**
	 * Call this method every time in cron schedule
	 */
	public void generateCSVFile(final String conversionCurrency)
	{
		List<String> currencyList = this.returnCurrencyPair(conversionCurrency);
		for (String currencyPair : currencyList) {
			List<ChartData> chartDataList = this.returnChartData(currencyPair);
			fileManager.writeCSV(currencyPair, chartDataList);
		}
		
	}

	public void execute(final String conversionCurrency)
	{
		List<String> currencyList = this.returnCurrencyPair(conversionCurrency);
		if(currencyList !=null && currencyList.size() > 0)
		{
			for (String currencyPair : currencyList) {

				List<ChartData> chartDataList = null;
				int i = 0;
				//retry if failed logic
				while (i < 4) {
					chartDataList = this.returnChartData(currencyPair);
					if (chartDataList != null) {
						break;
					}
					System.out.println("retrying for currency pair " + conversionCurrency + " as last attempt failed");
					i++;
				}

				fileManager.writeCSV(currencyPair, chartDataList);

				sleep();
			}
		}
		else
		{
			System.out.println("No hay datos");
		}
		
	}

	public void execute(String fromDate, String toDate, final String conversionCurrency, final StrategyHolder strategyHolder, final List<String> currencyIncludeList, final List<String> currencySkipList)
	{

		final List<ChartData> conversionCurrencyChartData = generateChartData(fromDate, toDate, conversionCurrency);
		fileManager.writeCSV(fromDate, toDate, conversionCurrency, conversionCurrencyChartData);
		final TimeSeries conversionTimeSeries = CsvTicksLoader.loadSeriesFromChartData(conversionCurrencyChartData, false, null);

		List<String> currencyList = this.returnCurrencyPair(conversionCurrency);
		currencyList = GenericUtils.filterCurrencyList(currencyList, currencyIncludeList, currencySkipList);

		if(currencyList !=null && currencyList.size() > 0)
		{
			for (String currencyPair : currencyList)
			{
				if (currencyPair.equalsIgnoreCase(conversionCurrency)) {
					continue;
				}

				final List<ChartData> chartDataList = generateChartData(fromDate, toDate, currencyPair);
				final TimeSeries timeSeries = CsvTicksLoader.loadSeriesFromChartData(chartDataList, true, conversionTimeSeries);

				if (strategyHolder != null) {
					runStrategy(currencyPair, timeSeries, chartDataList, strategyHolder);
				}

				fileManager.writeCSV(fromDate, toDate, currencyPair, chartDataList);
			}
		}
		else
		{
			System.out.println("No hay datos");
		}

	}

	private List<ChartData> generateChartData(String fromDate, String toDate, final String currencyPair) {
		List<ChartData> chartDataList = null;
		int i = 0;
		while (i < 4) {
			chartDataList = this.returnChartDataFromDateToDate(fromDate, toDate, currencyPair);
			if (chartDataList != null) {
				break;
			}
			System.out.println("retrying for currency pair " + currencyPair + " as last attempt failed");
			i++;
		}

		return chartDataList;

	}

	private void sleep() {
		try {
			Thread.sleep(500);
		} catch (final Exception e) {

		}
	}

}
