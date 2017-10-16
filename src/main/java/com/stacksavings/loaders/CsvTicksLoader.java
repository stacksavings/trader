package com.stacksavings.loaders;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.stacksavings.client.api.dto.ChartData;
import eu.verdelhan.ta4j.Decimal;
import javafx.scene.chart.Chart;
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

    private final static int SHOULD_ENTER_INDEX = 8;
    private final static int SHOULD_EXIT_INDEX = 9;

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
    
    /**
     * 
     * @param fileName
     * @return
     */
    public TimeSeries loadSeriesByFileName(final String fileName, final boolean useConversionSeries, final TimeSeries conversionSeries, final List<Map<String, Boolean>> buySellCacheForCurrency) {

		File file = new File(fileName);
		
		List<Tick> ticks = new ArrayList<>();
		
		CSVReader csvReader = null;
        try {

            InputStream stream = new FileInputStream(file);

            csvReader = new CSVReader(new InputStreamReader(stream, Charset.forName("UTF-8")), ',', '"', 0);

            int iter = 0;
            for (final String[] line : csvReader.readAll()) {
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

                    if (buySellCacheForCurrency != null) {
                        final Map<String, Boolean> buySellCacheMap = new HashMap<String, Boolean>();
                        final boolean shouldEnter = Boolean.valueOf(line[SHOULD_ENTER_INDEX]);
                        final boolean shouldExit = Boolean.valueOf(line[SHOULD_EXIT_INDEX]);

                        buySellCacheMap.put("shouldenter", shouldEnter);
                        buySellCacheMap.put("shouldexit", shouldExit);
                        buySellCacheForCurrency.add(buySellCacheMap);
                    }

                    iter++;
                }
            }
        }
        catch (final Exception e) {
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

        return new TimeSeries("loadSeriesByFileName", ticks);
    }

    public static TimeSeries loadSeriesFromChartData(final List<ChartData> chartDataList, final boolean useConversionSeries, final TimeSeries conversionSeries) {

        List<Tick> ticks = new ArrayList<>();

        int iter = 0;
        for (final ChartData chartData : chartDataList)
        {
            final String[] line = new String[6];

            DateTime date = DateTime.parse(chartData.getDate(), DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));

            line[OPEN_INDEX] = chartData.getOpen() + "";
            line[HIGH_INDEX] = chartData.getHigh() + "";
            line[LOW_INDEX] = chartData.getLow() + "";
            line[CLOSE_INDEX] = chartData.getClose() + "";
            line[VOLUME_INDEX] = chartData.getVolume() + "";

            double open = getValue(line, iter, OPEN_INDEX, useConversionSeries, conversionSeries);
            double high = getValue(line, iter, HIGH_INDEX, useConversionSeries, conversionSeries);
            double low = getValue(line, iter, LOW_INDEX, useConversionSeries, conversionSeries);
            double close = getValue(line, iter, CLOSE_INDEX, useConversionSeries, conversionSeries);
            double volume = getValue(line, iter, VOLUME_INDEX, useConversionSeries, conversionSeries);

            ticks.add(new Tick(date, open, high, low, close, volume));
            iter++;
        }

        return new TimeSeries("loadSeriesFromChartData", ticks);
    }

    private static double getValue(final String[] line, final int iter, final int index, final boolean useConversionSeries, final TimeSeries conversionSeries) {

        double retDouble = 0;
        if (useConversionSeries) {
            retDouble = convertUsingConversionSeries(line, iter, index, conversionSeries);
        } else {
            retDouble = Double.parseDouble(line[index]);
        }

        return retDouble;

    }

    private static double convertUsingConversionSeries(final String[] line, final int iter, final int index, final TimeSeries conversionSeries) {
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
