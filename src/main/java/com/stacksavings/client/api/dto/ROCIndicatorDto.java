package com.stacksavings.client.api.dto;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Tick;

/**
 * 
 * @author jpcol
 *
 */
public class ROCIndicatorDto implements Comparable<ROCIndicatorDto> {

	private Tick tick;
	private Decimal decimal;

	public ROCIndicatorDto(Tick tick, Decimal decimal)
	{
		this.tick = tick;
		this.decimal = decimal;
	}
	
	public Tick getTick() {
		return tick;
	}

	public void setTick(Tick tick) {
		this.tick = tick;
	}

	public Decimal getDecimal() {
		return decimal;
	}

	public void setDecimal(Decimal decimal) {
		this.decimal = decimal;
	}

	@Override
	/**
	 * Sorty by tick end time
	 */
	public int compareTo(ROCIndicatorDto rocIndicatorDto)
	{
		return rocIndicatorDto.getTick().getEndTime().compareTo(this.getTick().getEndTime());
	}

}
