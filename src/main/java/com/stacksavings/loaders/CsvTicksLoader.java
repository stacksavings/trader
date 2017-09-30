package com.stacksavings.loaders;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import eu.verdelhan.ta4j.Decimal;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import com.opencsv.CSVReader;
import com.stacksavings.utils.Constants;
import com.stacksavings.utils.PropertiesUtil;

import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;

/**
 * This class build a Ta4j time series from a CSV file containing ticks.
 * @author jpcol
 */
public class CsvTicksLoader {

    private final static int DATE_TIME_INDEX = 0;
    private final static int OPEN_INDEX = 1;
    private final static int HIGH_INDEX = 2;
    private final static int LOW_INDEX = 3;
    private final static int CLOSE_INDEX = 4;
    private final static int VOLUME_INDEX = 5;

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

	//TODO this should possibly be deprecated or removed
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
                //ZonedDateTime date = LocalDate.parse(line[0], DATE_FORMAT).atStartOfDay(ZoneId.systemDefault());
            	DateTime date = DateTime.parse(line[DATE_TIME_INDEX], DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));

                double open = Double.parseDouble(line[OPEN_INDEX]);
                double high = Double.parseDouble(line[HIGH_INDEX]);
                double low = Double.parseDouble(line[LOW_INDEX]);
                double close = Double.parseDouble(line[CLOSE_INDEX]);
                double volume = Double.parseDouble(line[VOLUME_INDEX]);

                ticks.add(new Tick(date, open, high, low, close, volume));
            }
        } catch (IOException ioe) {
            Logger.getLogger(CsvTicksLoader.class.getName()).log(Level.SEVERE, "Unable to load ticks from CSV", ioe);
        } catch (NumberFormatException nfe) {
            Logger.getLogger(CsvTicksLoader.class.getName()).log(Level.SEVERE, "Error while parsing value", nfe);
        }

        //TODO: name doesn't seem right
        return new TimeSeries("apple_ticks", ticks);
    }
    
    /**
     * 
     * @param fileName
     * @return
     */
    public TimeSeries loadSeriesByFileName(final String fileName, final boolean useConversionSeries, final TimeSeries conversionSeries) {

		File file = new File(fileName);
		
		List<Tick> ticks = new ArrayList<>();
		
		CSVReader csvReader = null;
        try {
        	
        	InputStream stream = new FileInputStream(file); 

            csvReader = new CSVReader(new InputStreamReader(stream, Charset.forName("UTF-8")), ',', '"', 1);
        	
            String[] line;
            int iter = 0;
            while ((line = csvReader.readNext()) != null) 
            {
                //ZonedDateTime date = LocalDate.parse(line[0], DATE_FORMAT).atStartOfDay(ZoneId.systemDefault());
                // ZonedDateTime date = ZonedDateTime.parse(line[0]).withZoneSameInstant(ZoneId.systemDefault());
                DateTime date = DateTime.parse(line[DATE_TIME_INDEX], DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
                
                double open = getValue(line, iter, OPEN_INDEX, useConversionSeries, conversionSeries);
                double high = getValue(line, iter, HIGH_INDEX, useConversionSeries, conversionSeries);
                double low = getValue(line, iter, LOW_INDEX, useConversionSeries, conversionSeries);
                double close = getValue(line, iter, CLOSE_INDEX, useConversionSeries, conversionSeries);
                double volume = getValue(line, iter, VOLUME_INDEX, useConversionSeries, conversionSeries);

                ticks.add(new Tick(date, open, high, low, close, volume));

                iter++;
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

		//TODO: name doesn't seem right
        return new TimeSeries("apple_ticks", ticks);
    }

    private double getValue(final String[] line, final int iter, final int index, final boolean useConversionSeries, final TimeSeries conversionSeries) {

        double retDouble = 0;
        if (useConversionSeries) {
            retDouble = convertUsingConversionSeries(line, iter, index, conversionSeries);
        } else {
            retDouble = Double.parseDouble(line[index]);
        }

        return retDouble;

    }

    private double convertUsingConversionSeries(final String[] line, final int iter, final int index, final TimeSeries conversionSeries) {
        final Double doubleValue = Double.parseDouble(line[index]);

        final Tick conversionTick = conversionSeries.getTick(iter);

        Decimal conversionRate = null;

        switch (index) {
            case OPEN_INDEX:
                conversionRate = conversionTick.getOpenPrice();
            case HIGH_INDEX:
                conversionRate = conversionTick.getMaxPrice();
            case LOW_INDEX:
                conversionRate = conversionTick.getMinPrice();
            case CLOSE_INDEX:
                conversionRate = conversionTick.getClosePrice();
        }

        double convertedDouble = doubleValue.doubleValue();

        if (conversionRate != null) {
            convertedDouble = doubleValue.doubleValue() * conversionRate.toDouble();
        }
        return convertedDouble;
    }

}
