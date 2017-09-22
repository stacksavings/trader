package com.stacksavings.poloniex.api;

import java.math.BigDecimal;

import org.junit.Test;

import com.stacksavings.utils.AutomatedTrader;

/**
 * 
 * @author jpcol
 *
 */
public class PoloniexApiBuyTest 
{

	
	@Test
	public void test()
	{
			
		String currencyPair = "USDT_BCH";
		BigDecimal buyPrice = BigDecimal.valueOf(420L);
		
		AutomatedTrader.getInstance().buy(currencyPair, buyPrice);
		
	}
}
