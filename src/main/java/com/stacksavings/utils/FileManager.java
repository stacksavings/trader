package com.stacksavings.utils;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
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
	 * 
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
				
				PrintWriter out = new PrintWriter(new FileWriter(directoryPath+currencyPair+"_"+fileName+"_"+dateNow+"."+filenameExtension, true));
				
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

}
