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
			
		String currencyPair = "BTC_STEEM";
		BigDecimal buyPrice = BigDecimal.valueOf(0.00028306);
		
		//PoloniexTraderClient.getInstance().buy(currencyPair, buyPrice, BigDecimal.ONE);
		
	}
}
