package com.stacksavings.utils;

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
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.opencsv.CSVReader;
import com.stacksavings.client.api.dto.ChartData;

/**
 * 
 * @author jpcol
 *
 */
public class FileManager {

	private static FileManager instance = null;
	private PropertiesUtil propertiesUtil;
	
	public static FileManager getInstance() 
	{
	      if(instance == null) 
	      {
	         instance = new FileManager();
	      }
	      
	      return instance;
	}	
	
	private FileManager	()
	{
		
		propertiesUtil = PropertiesUtil.getInstance();
		
	}
	

	
	/**
	 * This method write a csv file
	 * @param chartDataList
	 */
	public void writeCSV(String currencyPair, List<ChartData> chartDataList) {

		if(chartDataList != null && chartDataList.size()>0)
		{
			try 
			{
				String directoryPath = propertiesUtil.getProps().getProperty("path.directory");
				String fileName = propertiesUtil.getProps().getProperty("filename");
				String filenameExtension = propertiesUtil.getProps().getProperty("filename.extension");
				
				SimpleDateFormat sdf = new SimpleDateFormat(Constants.YYYY_MM_DD);
				
				String dateNow = sdf.format(new Date());
				
				createDirectory(dateNow);
				
				PrintWriter out = new PrintWriter(new FileWriter(directoryPath+"//"+dateNow+"//"+currencyPair+"_"+fileName+"_"+dateNow+"."+filenameExtension, true));
				
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
	 * This method create a directory if it doesn't exists
	 * @param directoryName
	 */
	public void createDirectory(String directoryName)
	{
		String directoryPath = propertiesUtil.getProps().getProperty("path.directory");
		File file = new File(directoryPath+directoryName);
		
		if(file.isDirectory() && !file.exists()){
			file.mkdir();
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public String getLastDateFromCSVFile(String currencyPair)
	{
		String directoryPath = propertiesUtil.getProps().getProperty("path.directory");
		String fileName = propertiesUtil.getProps().getProperty("filename");
		String filenameExtension = propertiesUtil.getProps().getProperty("filename.extension");
		
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.YYYY_MM_DD);
		
		SimpleDateFormat sdTime = new SimpleDateFormat(Constants.YYYY_MM_DD_HH_MM_SS);
		
		Date date= new Date();
		
		String dateNow = sdf.format(date);
		
		File file = new File(directoryPath+fileName+"_"+dateNow+"."+filenameExtension);
		
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MINUTE, -1200);
		
		String dateNowTime = sdTime.format(new Date(calendar.getTimeInMillis()));
		
		String resultFinal = dateNowTime;
		
		if(file.exists() && !file.isDirectory()) { 
		    // recuperar el �ltimo registro 
	        try {
				
	        	InputStream stream = new FileInputStream(file); 
			    CSVReader csvReader = new CSVReader(new InputStreamReader(stream, Charset.forName("UTF-8")), ',', '"', 1);
	            String[] line;
	            String[] lineAux = null;
	            
	            List<String[]> allLines = csvReader.readAll();
	            
	            lineAux = allLines.get(allLines.size()-1); // Get last line
	            
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

}
