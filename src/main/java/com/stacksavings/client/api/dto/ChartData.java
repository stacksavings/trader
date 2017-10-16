package com.stacksavings.client.api.dto;

import com.stacksavings.loaders.CsvTicksLoader;
import eu.verdelhan.ta4j.Tick;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 
 * @author jpcol
 *
 */
public class ChartData {
	
	private String date; // 1439006400,
	private double high; // : 50,
	private double low; // : 0.0045001,
	private double open; // : 50,
	private double close; // : 0.004555,
	private double volume;// : 329.6493784,
	private double quoteVolume; // : 54434.7809242,
	private double weightedAverage; // : 0.00605585
	private boolean strategyShouldEnter;
	private boolean strategyShouldExit;

	private double conv_high;
	private double conv_low;
	private double conv_open;
	private double conv_close;
	private double conv_volume;

	private static String DELIMITER = ",";

	public String getDate() {
		
		Date dDate=new Date(Long.parseLong(date)*1000);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
//		Instant instant = dDate.toInstant();
//		ZonedDateTime LAZone= instant.atZone(ZoneId.systemDefault());
		//System.out.println("In Los Angeles(America) Time Zone:"+ LAZone);
		//ZoneId.systemDefault()
		// dt.setTimeZone(ZoneId.systemDefault());
		//return dt.format(dDate);
		//return LAZone.toString();
		return sdf.format(dDate);
		
	}

	public void setDate(String date) {
		this.date = date;
	}

	public double getHigh() {
		return high;
	}

	public void setHigh(double high) {
		this.high = high;
	}

	public double getLow() {
		return low;
	}

	public void setLow(double low) {
		this.low = low;
	}

	public double getOpen() {
		return open;
	}

	public void setOpen(double open) {
		this.open = open;
	}

	public double getClose() {
		return close;
	}

	public void setClose(double close) {
		this.close = close;
	}

	public double getVolume() {
		return volume;
	}

	public void setVolume(double volume) {
		this.volume = volume;
	}

	public double getQuoteVolume() {
		return quoteVolume;
	}

	public void setQuoteVolume(double quoteVolume) {
		this.quoteVolume = quoteVolume;
	}

	public double getWeightedAverage() {
		return weightedAverage;
	}

	public void setWeightedAverage(double weightedAverage) {
		this.weightedAverage = weightedAverage;
	}


	public double getConv_high() {
		return conv_high;
	}

	public double getConv_low() {
		return conv_low;
	}

	public double getConv_open() {
		return conv_open;
	}

	public double getConv_close() {
		return conv_close;
	}

	public double getConv_volume() {
		return conv_volume;
	}

	public void generateConvertedValues(final Tick conversionTick) {

		conv_high = CsvTicksLoader.convertUsingConversionTick(getHigh(), CsvTicksLoader.HIGH_INDEX, conversionTick);
		conv_low = CsvTicksLoader.convertUsingConversionTick(getHigh(), CsvTicksLoader.LOW_INDEX, conversionTick);
		conv_open = CsvTicksLoader.convertUsingConversionTick(getHigh(), CsvTicksLoader.OPEN_INDEX, conversionTick);
		conv_close = CsvTicksLoader.convertUsingConversionTick(getHigh(), CsvTicksLoader.CLOSE_INDEX, conversionTick);
		//TODO check if volume conversion makes sense
		conv_volume = CsvTicksLoader.convertUsingConversionTick(getHigh(), CsvTicksLoader.VOLUME_INDEX, conversionTick);

	}

	public boolean strategyShouldEnter() {
		return strategyShouldEnter;
	}

	public void setStrategyShouldEnter(boolean strategyShouldEnter) {
		this.strategyShouldEnter = strategyShouldEnter;
	}

	public boolean strategyShouldExit() {
		return strategyShouldExit;
	}

	public void setStrategyShouldExit(boolean strategyShouldExit) {
		this.strategyShouldExit = strategyShouldExit;
	}

	@Override
	public String toString()
	{
		return getDate() + DELIMITER
				+ getHigh() + DELIMITER
				+ getLow() + DELIMITER
				+ getOpen() + DELIMITER
				+ getClose() + DELIMITER
				+ getVolume() + DELIMITER
				+ getQuoteVolume() + DELIMITER
				+ getWeightedAverage() + DELIMITER
				+ strategyShouldEnter() + DELIMITER
				+ strategyShouldExit() + DELIMITER

				+ getConv_high() + DELIMITER
				+ getConv_low() + DELIMITER
				+ getConv_open() + DELIMITER
				+ getConv_close() + DELIMITER
				+ getConv_volume() + DELIMITER;

	}

}
