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
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

	private static FileManager instance ;
	
	private PropertiesUtil propertiesUtil;
	
	private LoggerManager loggerManager;
	
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
		
		loggerManager = LoggerManager.getInstance();
		
	}
	
	/**
	 * 
	 * @param currencyPair
	 * @return
	 */
	public String getFileNameByCurrency(String currencyPair)
	{
		String directoryPath = propertiesUtil.getProps().getProperty("path.directory");
		String fileName = propertiesUtil.getProps().getProperty("filename");
		String filenameExtension = propertiesUtil.getProps().getProperty("filename.extension");
		
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.YYYY_MM_DD);
		
		String dateNow = sdf.format(new Date());
		
		return directoryPath+"//"+dateNow+"//"+currencyPair+"_"+fileName+"_"+dateNow+"."+filenameExtension;

	}
	
	/**
	 * Build a File Object from properties 
	 * 
	 * @param currencyPair
	 * @return
	 */
	private File getFileByName(String currencyPair)
	{
		String directoryPath = propertiesUtil.getProps().getProperty("path.directory");
		String fileName = propertiesUtil.getProps().getProperty("filename");
		String filenameExtension = propertiesUtil.getProps().getProperty("filename.extension");
		
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.YYYY_MM_DD);
		
		String dateNow = sdf.format(new Date());
		
		File file = new File(directoryPath+"//"+dateNow+"//"+currencyPair+"_"+fileName+"_"+dateNow+"."+filenameExtension);
		
		return file;
	}
	
	
	/**
	 * Build a File Object from properties 
	 * 
	 * @param fromDate
	 * @param toDate
	 * @param currencyPair
	 * @return
	 */
	private File getFileByName(String fromDate, String toDate, String currencyPair)
	{
		String directoryPath = propertiesUtil.getProps().getProperty("path.directory");
		String fileName = propertiesUtil.getProps().getProperty("filename");
		String filenameExtension = propertiesUtil.getProps().getProperty("filename.extension");
		
		SimpleDateFormat sdf2 = new SimpleDateFormat(Constants.YYYY_MM_DD);
		
		//String dateNow = sdf.format(new Date());
		
		SimpleDateFormat sdf =new SimpleDateFormat(Constants.YYYY_MM_DD_HH_MM_SS);
		
		try {
			Date dFromDate = sdf.parse(fromDate);
			
			Date dToDate = sdf.parse(toDate);
			
			String sFromDate = sdf2.format(dFromDate);
			
			String sToDate = sdf2.format(dToDate);
			
			File file = new File(directoryPath+"//"+sFromDate+"_"+sToDate+"//"+currencyPair+"_"+fileName+"_"+sFromDate+"_"+sToDate+"."+filenameExtension);
		
			return file;
		} 
		catch (ParseException e) 
		{
			e.printStackTrace();
		}
		
		return null;
	}
	
	/**
	 * This method write a csv file
	 * @param chartDataList
	 */
	public void writeCSV(String currencyPair, List<ChartData> chartDataList) {

		if(chartDataList != null && chartDataList.size()>0)
		{
			PrintWriter out = null;
			try 
			{
				SimpleDateFormat sdf = new SimpleDateFormat(Constants.YYYY_MM_DD);
				
				String dateNow = sdf.format(new Date());
				
				createDirectory(dateNow);
				
				File file = getFileByName(currencyPair);
				
				out = new PrintWriter(new FileWriter(file, true));
				
				for (ChartData chartData : chartDataList) 
				{
					if(chartData.getClose() != 0d && chartData.getHigh() != 0d && 
					   chartData.getLow() != 0d && chartData.getOpen() != 0d && 
					   chartData.getQuoteVolume() != 0 && chartData.getVolume() != 0d)
					{
						out.println(chartData.toString());
					}
					
				}
			} 
			catch (FileNotFoundException e) 
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			finally
			{
				if(out != null)
				{
					out.close();
				}
			}
		}
	}
	
	/**
	 * This method write a csv file
	 * @param chartDataList
	 */
	public void writeCSV(String fromDate, String toDate, String currencyPair, List<ChartData> chartDataList) 
	{

		if(chartDataList != null && chartDataList.size()>0)
		{
			PrintWriter out = null;
			try 
			{
				// SimpleDateFormat sdf = new SimpleDateFormat(Constants.YYYY_MM_DD);
				
				// String dateNow = sdf.format(new Date());
				
				createDirectory(fromDate, toDate);
				
				File file = getFileByName(fromDate, toDate, currencyPair);
				
				out = new PrintWriter(new FileWriter(file, true));
				
				for (ChartData chartData : chartDataList) 
				{
					if(chartData.getClose() != 0d && chartData.getHigh() != 0d && 
					   chartData.getLow() != 0d && chartData.getOpen() != 0d && 
					   chartData.getQuoteVolume() != 0 && chartData.getVolume() != 0d)
					{
						out.println(chartData.toString());
					}
					
				}
			} 
			catch (FileNotFoundException e) 
			{
				e.printStackTrace();
			} 
			catch (IOException e) 
			{
				e.printStackTrace();
			}
			finally
			{
				if(out != null)
				{
					out.close();
				}
			}
		}
	}
	
	
	/**
	 * This method create a directory if it doesn't exists
	 * 
	 * @param fromDate
	 * @param toDate
	 */
	public void createDirectory(String fromDate, String toDate)
	{
		String directoryPath = propertiesUtil.getProps().getProperty("path.directory");
		
		
		SimpleDateFormat sdf2 = new SimpleDateFormat(Constants.YYYY_MM_DD);
		
		//String dateNow = sdf.format(new Date());
		
		SimpleDateFormat sdf =new SimpleDateFormat(Constants.YYYY_MM_DD_HH_MM_SS);
		
		try {
			Date dFromDate = sdf.parse(fromDate);
			
			Date dToDate = sdf.parse(toDate);
			
			String sFromDate = sdf2.format(dFromDate);
			
			String sToDate = sdf2.format(dToDate);

			File file = new File(directoryPath+"//"+sFromDate+"_"+sToDate);
			
			if(!file.exists())
			{
				file.mkdir();
			}
		}
		catch(ParseException e)
		{
			e.printStackTrace();
		}
		

	}
	
	
	/**
	 * This method create a directory if it doesn't exists
	 * 
	 * @param fromDate
	 * @param toDate
	 */
	public void createDirectory(String directoryName)
	{
		String directoryPath = propertiesUtil.getProps().getProperty("path.directory");
		
		File file = new File(directoryPath+"//"+directoryName);
		
		if(!file.exists()){
			file.mkdir();
		}
	}
	
	/**
	 * This method remove a directory 
	 * @param directoryName
	 */
	public void removeDirectory(String directoryName)
	{
		String directoryPath = propertiesUtil.getProps().getProperty("path.directory");
		File file = new File(directoryPath+"//"+directoryName);
		
		if(file.isDirectory())
		{
			for (File c : file.listFiles())
			{
				c.delete();
			}
			file.delete();
		}
	}
	
	/**
	 * Remove directory not equal today
	 */
	public void clearDirectory()
	{
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.YYYY_MM_DD);
		String dateNow = sdf.format(new Date());

		String directoryPath = propertiesUtil.getProps().getProperty("path.directory");
		File fileDirectory = new File(directoryPath);
		
		if (fileDirectory.listFiles() != null)  
		{
			for(File file :fileDirectory.listFiles())
			{
				String name = file.getName();
				if(!name.equals(dateNow))
				{
					loggerManager.info("directory will be removed: "+ dateNow);

					removeDirectory(name);
				}
			}
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public String getLastDateFromCSVFile(String currencyPair)
	{
		File file = getFileByName(currencyPair);
				
		SimpleDateFormat sdTime = new SimpleDateFormat(Constants.YYYY_MM_DD_HH_MM_SS);
		
		Date date= new Date();
				
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);
		calendar.add(Calendar.MINUTE, -2400);
		
		String dateNowTime = sdTime.format(new Date(calendar.getTimeInMillis()));
		
		// String resultFinal = ZonedDateTime.now().withZoneSameLocal(ZoneId.systemDefault()).minusMinutes(2400).toString();
		String resultFinal = dateNowTime;
		
		if(file.exists() && !file.isDirectory()) { 
		    // recuperar el ultimo registro
			CSVReader csvReader = null;
	        try {
				
	        	InputStream stream = new FileInputStream(file); 
			    csvReader = new CSVReader(new InputStreamReader(stream, Charset.forName("UTF-8")), ',', '"', 1);
	            String[] lineAux = null;
	            
	            List<String[]> allLines = csvReader.readAll();
	            
	            if((allLines.size() >0 && (allLines.size()-1) < allLines.size())){
		            lineAux = allLines.get(allLines.size()-1); // Get last line
		            
		            Date lastDate = sdTime.parse(lineAux[0]);
		            Calendar calendar2 = Calendar.getInstance();
		    		calendar2.setTime(lastDate);
		    		calendar2.add(Calendar.MINUTE, 5);
		    		
		    		//ZonedDateTime zonedDateTime = ZonedDateTime.parse(lineAux[0]);
		    		//zonedDateTime = zonedDateTime.plusMinutes(5L);
		    		
		    		resultFinal = sdTime.format(calendar2.getTime());
		    		
	            }
	            
	        } 
	        catch (IOException ioe) 
	        {
	        	ioe.printStackTrace();
	        } 
	        catch (NumberFormatException nfe) 
	        {
	        	nfe.printStackTrace();
	        } 
	        catch (ParseException e) 
	        {
				e.printStackTrace();
			} 
	        finally{
	        	if(csvReader!=null ){
	        		try 
	        		{
						csvReader.close();
					} 
	        		catch (IOException e) 
	        		{
						e.printStackTrace();
					}
	        	}
			}			
			
		}
		return resultFinal;
	}

}
