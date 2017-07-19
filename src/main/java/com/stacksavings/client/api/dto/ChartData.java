package com.stacksavings.client.api.dto;

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

	public String getDate() {
		
		Date dDate=new Date(Long.parseLong(date)*1000);
		SimpleDateFormat dt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dt.format(dDate);
		
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
	
	@Override
	public String toString(){
		/*
		private String date; // 1439006400,
		private int high; // : 50,
		private double low; // : 0.0045001,
		private int open; // : 50,
		private double close; // : 0.004555,
		private double volume;// : 329.6493784,
		private double quoteVolume; // : 54434.7809242,
		private double weightedAverage; // : 0.00605585 */
		
		return getDate()+","+getHigh()+","+getLow()+","+getOpen()+","+getClose()+","+getVolume()+","+getQuoteVolume()+","+getWeightedAverage();
	}

}
