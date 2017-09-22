package com.stacksavings.poloniex.api;

import java.math.BigDecimal;

import org.junit.Test;

import com.stacksavings.utils.PoloniexTraderClient;

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
			
		String currencyPair = "BTC_BCH";
		BigDecimal buyPrice = BigDecimal.valueOf(0.000574405);
		
		PoloniexTraderClient.getInstance().buy(currencyPair, buyPrice);
		
	}
}
