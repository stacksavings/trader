package com.stacksavings.client.api;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVWriter;
import com.opencsv.bean.BeanToCsv;
import com.opencsv.bean.ColumnPositionMappingStrategy;
import com.stacksavings.client.api.dto.ChartData;

public class PoloniexClientApi {

	public final static String ENDPOINT_API = "https://poloniex.com/public";
	public final static String RETURN_CHART_DATA = "?command=returnChartData&currencyPair=BTC_ETH&start=1435699200&end=1498867200&period=14400";

	public List<ChartData> consumeData() {
		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet request = new HttpGet(ENDPOINT_API + RETURN_CHART_DATA);
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

	public void writeCSV(List<ChartData> chartDataList) {

		CSVWriter csvWriter;
		try {
			csvWriter = new CSVWriter(new FileWriter("src/main/resources/chartData.csv"));
			
			BeanToCsv<ChartData> bc = new BeanToCsv<ChartData>();

			// mapping of columns with their positions
			ColumnPositionMappingStrategy<ChartData> mappingStrategy = new ColumnPositionMappingStrategy<ChartData>();
			// Set mappingStrategy type to Employee Type
			mappingStrategy.setType(ChartData.class);
			// Fields in Employee Bean
			String[] columns = new String[] { "date", "high", "low", "open","close","volume","quoteVolume", "weightedAverage" };
			
			// Setting the colums for mappingStrategy
			mappingStrategy.setColumnMapping(columns);
			// Writing empList to csv file
			bc.write(mappingStrategy, csvWriter, chartDataList);
			
			System.out.println("CSV File written successfully!!!");

		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public void execute()
	{
		List<ChartData> chartDataList = this.consumeData();
		writeCSV(chartDataList);
	}

	public static void main(String[] args) {

		new PoloniexClientApi().execute();
		
	}
}
