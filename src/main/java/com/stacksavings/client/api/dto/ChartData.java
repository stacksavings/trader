package com.stacksavings.client.api.dto;

/**
 * 
 * @author jpcol
 *
 */
public class ChartData {
	
	private long date; // 1439006400,
	private int high; // : 50,
	private double low; // : 0.0045001,
	private int open; // : 50,
	private double close; // : 0.004555,
	private double volume;// : 329.6493784,
	private double quoteVolume; // : 54434.7809242,
	private double weightedAverage; // : 0.00605585

	public long getDate() {
		return date;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public int getHigh() {
		return high;
	}

	public void setHigh(int high) {
		this.high = high;
	}

	public double getLow() {
		return low;
	}

	public void setLow(double low) {
		this.low = low;
	}

	public int getOpen() {
		return open;
	}

	public void setOpen(int open) {
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

}
