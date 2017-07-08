package com.stacksavings.client.api;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stacksavings.client.api.dto.ChartDataMain;

public class PoloniexClientApi {
	
	public final static String BASE_API="https://poloniex.com/public";
	public final static String RETURN_CHART_DATA="?command=returnChartData&currencyPair=BTC_ETH&start=1435699200&end=1498867200&period=14400";
	
	public void consumeData(){
		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet request = new HttpGet(BASE_API+RETURN_CHART_DATA);
		HttpResponse response;
		try {
			response = client.execute(request);
			HttpEntity entity1 = response.getEntity();
			byte[] byteData= EntityUtils.toByteArray(entity1);
			
			ObjectMapper objectMapper = new ObjectMapper();
			
			ChartDataMain chartDataMain = objectMapper.readValue(byteData, ChartDataMain.class);
			
			System.out.println("chartDataMainObject\n"+chartDataMain);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		
		new PoloniexClientApi().consumeData();
	}
}
