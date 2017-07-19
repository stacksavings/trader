package com.stacksavings.client.api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.stacksavings.client.api.dto.ChartData;
import com.stacksavings.utils.Constants;
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
	
	/**
	 * 
	 * @return
	 */
	public String getLastDate()
	{
		String directoryPath = propertiesUtil.getProps().getProperty("path.directory");
		String fileName = propertiesUtil.getProps().getProperty("filename");
		String filenameExtension = propertiesUtil.getProps().getProperty("filename.extension");
		
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.YYYY_MM_DD);
		
		SimpleDateFormat sdTime = new SimpleDateFormat(Constants.YYYY_MM_DD_HH_MM_SS);
		
		Date date= new Date();
		
		String dateNow = sdf.format(date);
		
		File f = new File(directoryPath+fileName+"_"+dateNow+"."+filenameExtension);
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MINUTE, -30);
		
		String dateNowTime = sdTime.format(new Date(calendar.getTimeInMillis()));
		
		String resultFinal = dateNowTime;
		
		if(f.exists() && !f.isDirectory()) { 
		    // recuperar el último registro 
	        try {
				
	        	InputStream stream = new FileInputStream(f); 
			    CSVReader csvReader = new CSVReader(new InputStreamReader(stream, Charset.forName("UTF-8")), ',', '"', 1);
	            String[] line;
	            String[] lineAux = null;
	            
	            List<String[]> allLines = csvReader.readAll();
	            
	            lineAux = allLines.get(allLines.size()-1);
	            
	            Date lastDate = sdTime.parse(lineAux[0]);
	            Calendar calendar2 = Calendar.getInstance();
	    		calendar2.setTime(lastDate);
	    		calendar2.add(Calendar.MINUTE, 5);
	    		
	            resultFinal = sdTime.format(calendar2.getTime());
	            
	        } catch (IOException ioe) {
	        	ioe.printStackTrace();
	        } catch (NumberFormatException nfe) {
	        	nfe.printStackTrace();
	        } catch (ParseException e) {
				e.printStackTrace();
			}			
			
		}
		return resultFinal;
	}
	
	/**
	 * 
	 * @return
	 */
	public List<ChartData> consumeData() 
	{
		CloseableHttpClient client = HttpClients.createDefault();
		String restApiService = propertiesUtil.getProps().getProperty("endpoint.api")+propertiesUtil.getProps().getProperty("return.chart.data");
		
		
		try {
			
			SimpleDateFormat sdf =new SimpleDateFormat(Constants.YYYY_MM_DD_HH_MM_SS);
			String sDate = getLastDate();
			
			Date dDate = sdf.parse(sDate);
			
			Date dDateNow = new Date();
						
			Long lDateBegin = dDate.getTime()/1000;
			
			restApiService = restApiService.replaceAll("startbegin", lDateBegin.toString() );
			
			Long lDateEnd = dDateNow.getTime()/1000;
			
			restApiService = restApiService.replaceAll("startend", lDateEnd.toString());
			
			HttpGet request = new HttpGet(restApiService);
			HttpResponse response;
			
			response = client.execute(request);
			HttpEntity entity1 = response.getEntity();
			byte[] byteData = EntityUtils.toByteArray(entity1);

			ObjectMapper objectMapper = new ObjectMapper();

			List<ChartData> chartDataList = Arrays.asList(objectMapper.readValue(byteData, ChartData[].class));

			return chartDataList;

		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 
	 * @param chartDataList
	 */
	public void writeCSV(List<ChartData> chartDataList) {

		if(chartDataList != null && chartDataList.size()>0)
		{
			try 
			{
				String directoryPath = propertiesUtil.getProps().getProperty("path.directory");
				String fileName = propertiesUtil.getProps().getProperty("filename");
				String filenameExtension = propertiesUtil.getProps().getProperty("filename.extension");
				
				SimpleDateFormat sdf = new SimpleDateFormat(Constants.YYYY_MM_DD);
				
				String dateNow = sdf.format(new Date());
				
				PrintWriter out = new PrintWriter(new FileWriter(directoryPath+fileName+"_"+dateNow+"."+filenameExtension, true));
				
				for (ChartData chartData : chartDataList) {
					out.println(chartData.toString());
				}
				out.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 */
	public void execute()
	{
		List<ChartData> chartDataList = this.consumeData();
		writeCSV(chartDataList);
	}

	/**
	 * 
	 * @param args
	 */
	public static void main(String[] args) {

		//System.out.println(PoloniexClientApi.getInstance().getLastDate());
		PoloniexClientApi.getInstance().execute();
	}
}
