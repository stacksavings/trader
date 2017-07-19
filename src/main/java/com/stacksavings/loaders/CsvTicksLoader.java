package com.stacksavings.loaders;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.opencsv.CSVReader;
import com.stacksavings.client.api.PoloniexClientApi;
import com.stacksavings.utils.Constants;
import com.stacksavings.utils.PropertiesUtil;

import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;

/**
 * This class build a Ta4j time series from a CSV file containing ticks.
 * @author jpcol
 */
public class CsvTicksLoader {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static CsvTicksLoader instance = null;
    /**
     * @return a time series from Apple Inc. ticks.
     */
    
	private PropertiesUtil propertiesUtil;

	public static CsvTicksLoader getInstance() 
	{
	      if(instance == null) 
	      {
	         instance = new CsvTicksLoader();
	      }
	      
	      return instance;
	}	
	
	private CsvTicksLoader()
	{
		
		propertiesUtil = PropertiesUtil.getInstance();
		
	}
    
    public TimeSeries loadSeries() {
    	
    	String directoryPath = propertiesUtil.getProps().getProperty("path.directory");
		String fileName = propertiesUtil.getProps().getProperty("filename");
		String filenameExtension = propertiesUtil.getProps().getProperty("filename.extension");
		
		SimpleDateFormat sdf = new SimpleDateFormat(Constants.YYYY_MM_DD);
		
		Date dateNow= new Date();
		
		String sDateNow = sdf.format(dateNow);
		
		File file = new File(directoryPath+fileName+"_"+sDateNow+"."+filenameExtension);
		
		List<Tick> ticks = new ArrayList<>();
		
        try {
        	
        	InputStream stream = new FileInputStream(file); 

            CSVReader csvReader = new CSVReader(new InputStreamReader(stream, Charset.forName("UTF-8")), ',', '"', 1);
        	
            String[] line;
            while ((line = csvReader.readNext()) != null) {
                ZonedDateTime date = LocalDate.parse(line[0], DATE_FORMAT).atStartOfDay(ZoneId.systemDefault());
                double open = Double.parseDouble(line[1]);
                double high = Double.parseDouble(line[2]);
                double low = Double.parseDouble(line[3]);
                double close = Double.parseDouble(line[4]);
                double volume = Double.parseDouble(line[5]);

                ticks.add(new Tick(date, open, high, low, close, volume));
            }
        } catch (IOException ioe) {
            Logger.getLogger(CsvTicksLoader.class.getName()).log(Level.SEVERE, "Unable to load ticks from CSV", ioe);
        } catch (NumberFormatException nfe) {
            Logger.getLogger(CsvTicksLoader.class.getName()).log(Level.SEVERE, "Error while parsing value", nfe);
        }

        return new TimeSeries("apple_ticks", ticks);
    }

    public static void main(String[] args) {
        TimeSeries series = CsvTicksLoader.getInstance().loadSeries();

        System.out.println("Series: " + series.getName() + " (" + series.getSeriesPeriodDescription() + ")");
        System.out.println("Number of ticks: " + series.getTickCount());
        System.out.println("First tick: \n"
                + "\tVolume: " + series.getTick(0).getVolume() + "\n"
                + "\tOpen price: " + series.getTick(0).getOpenPrice()+ "\n"
                + "\tClose price: " + series.getTick(0).getClosePrice());
    }
}
