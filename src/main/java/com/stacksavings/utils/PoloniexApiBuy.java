package com.stacksavings.utils;

import java.math.BigDecimal;

import com.cf.client.poloniex.PoloniexExchangeService;
import com.cf.data.model.poloniex.PoloniexOrderResult;
import com.cf.data.model.poloniex.PoloniexTradeHistory;

/**
 * 
 * @author jpcol
 *
 */
public class PoloniexApiBuy 
{

	
	/**
	 * 
	 * @param key
	 * @param secret
	 * @param currencyPair
	 * @param buyPrice
	 */
	public static void buy(String key, String secret, String currencyPair, BigDecimal buyPrice)
	{
		
		String apiKey = key;
		String apiSecret = secret;
		PoloniexExchangeService service = new PoloniexExchangeService(apiKey, apiSecret);

		BigDecimal amount = BigDecimal.ONE;
		boolean fillOrKill = false;
		boolean immediateOrCancel = false;
		boolean postOnly = false;
		PoloniexOrderResult buyOrderResult = 
		      service.buy(currencyPair, buyPrice, amount, fillOrKill, immediateOrCancel, postOnly);
		
		if(buyOrderResult.resultingTrades != null && 
				buyOrderResult.resultingTrades.size()> 0)
		{
			for (PoloniexTradeHistory poloniexTradeHistory : buyOrderResult.resultingTrades) 
			{
				System.out.println(poloniexTradeHistory.amount); 
			}
		}
	}
}
