package com.stacksavings.poloniex.api;

import java.math.BigDecimal;

import org.junit.Test;

import com.stacksavings.utils.PoloniexApiBuy;

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
			
		String apiKey = "AAI6UGTX-7JUNRH7Y-XACSMQIO-YH4MAO3A";
		String apiSecret = "b0f548aff578b23c95885b7d4e2fa04229d5c55f5b43aa14facbb24d43958f27d177978c2b8a459ed9b9fa45a809d098416e241453b9dc123e6bb81678e2ec4c";
		String currencyPair = "USDT_BTC";
		BigDecimal buyPrice = BigDecimal.valueOf(1980L);
		
		PoloniexApiBuy.buy(apiKey, apiSecret, currencyPair, buyPrice);
		
	}
}
