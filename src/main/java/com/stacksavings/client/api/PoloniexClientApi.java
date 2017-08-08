package com.stacksavings.client.api;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stacksavings.client.api.dto.ChartData;
import com.stacksavings.utils.Constants;
import com.stacksavings.utils.FileManager;
import com.stacksavings.utils.LoggerManager;
import com.stacksavings.utils.PropertiesUtil;

/**
 * 
 * @author jpcol <br>
 * This class is a singleton
 */
public class PoloniexClientApi {

	private static PoloniexClientApi instance = null;
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
	public List<String> returnCurrencyPair()
	{
		loggerManager.info("begin returnCurrencyPair");
		
		CloseableHttpClient client = HttpClients.createDefault();
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
				currencyPair.add(item);
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
		loggerManager.info("begin returnCurrencyPair");
		
		CloseableHttpClient client = HttpClients.createDefault();
		String restApiService = propertiesUtil.getProps().getProperty("endpoint.api")+propertiesUtil.getProps().getProperty("return.chart.data");
		
		try {
			
			SimpleDateFormat sdf =new SimpleDateFormat(Constants.YYYY_MM_DD_HH_MM_SS);
			
			String sDate = fileManager.getLastDateFromCSVFile(currencyPair);
			
			Date dDate = sdf.parse(sDate);
			
			Date dDateNow = new Date();
						
			Long lDateBegin = dDate.getTime()/1000;
			
			restApiService = restApiService.replaceAll("startbegin", lDateBegin.toString() );
			
			Long lDateEnd = dDateNow.getTime()/1000;
			
			restApiService = restApiService.replaceAll("startend", lDateEnd.toString());
			
			restApiService = restApiService.replaceAll("currency_pair", currencyPair);
			
			HttpGet request = new HttpGet(restApiService);
			HttpResponse response;
			
			response = client.execute(request);
			HttpEntity entity1 = response.getEntity();
			byte[] byteData = EntityUtils.toByteArray(entity1);

			ObjectMapper objectMapper = new ObjectMapper();

			List<ChartData> chartDataList = Arrays.asList(objectMapper.readValue(byteData, ChartData[].class));

			loggerManager.info("end returnCurrencyPair");
			
			return chartDataList;

		} catch (IOException e) {
			loggerManager.error(e.getMessage());
		} catch (ParseException e) {
			loggerManager.error(e.getMessage());
		}
		return null;
	}

	
	/**
	 * Call this method every time in cron schedule
	 */
	public void generateCSVFile()
	{
		List<String> currencyList = this.returnCurrencyPair();
		for (String currencyPair : currencyList) {
			List<ChartData> chartDataList = this.returnChartData(currencyPair);
			fileManager.writeCSV(currencyPair, chartDataList);
		}
		
	}
	
	/**
	 * 
	 * @param currencyPair
	 */
	public void execute(String currencyPair)
	{
		List<ChartData> chartDataList = this.returnChartData(currencyPair);
		fileManager.writeCSV(currencyPair, chartDataList);
	}

}
