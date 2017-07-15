package com.stacksavings.client.api;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stacksavings.client.api.dto.ChartData;
import com.stacksavings.utils.PropertiesUtil;

/**
 * 
 * @author jpcol <br>
 * This class is a singleton
 */
public class PoloniexClientApi {

	private static PoloniexClientApi instance = null;
	private PropertiesUtil propertiesUtil;
	
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
		
	}
	
	public List<ChartData> consumeData() {
		CloseableHttpClient client = HttpClients.createDefault();
		String restApiService = propertiesUtil.getProps().getProperty("endpoint.api")+propertiesUtil.getProps().getProperty("return.chart.data");
		HttpGet request = new HttpGet(restApiService);
		HttpResponse response;
		try {
			response = client.execute(request);
			HttpEntity entity1 = response.getEntity();
			byte[] byteData = EntityUtils.toByteArray(entity1);

			ObjectMapper objectMapper = new ObjectMapper();

			List<ChartData> chartDataList = Arrays.asList(objectMapper.readValue(byteData, ChartData[].class));

			return chartDataList;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * @param chartDataList
	 */
	public void writeCSV(List<ChartData> chartDataList) {

		try {
			String directoryPath = propertiesUtil.getProps().getProperty("path.directory");
			String fileName = propertiesUtil.getProps().getProperty("filename");
			String filenameExtension = propertiesUtil.getProps().getProperty("filename.extension");
			
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyy_mm_dd");
			
			String dateNow = sdf.format(new Date());
			
			PrintWriter out = new PrintWriter(directoryPath+fileName+dateNow+"."+filenameExtension);
			
			for (ChartData chartData : chartDataList) {
				out.println(chartData.toString());
			}
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}


	}
	
	public void execute()
	{
		List<ChartData> chartDataList = this.consumeData();
		writeCSV(chartDataList);
	}

	public static void main(String[] args) {

		PoloniexClientApi.getInstance().execute();
		
	}
}
