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
public class PoloniexTraderClient
{
	
	private static PoloniexTraderClient instance;
	
	private PropertiesUtil propertiesUtil;
	
	private String key; 
	
	private String secret;
		
	/**
	 * 
	 * @return
	 */
	public static PoloniexTraderClient getInstance()
	{
	      if(instance == null) 
	      {
	         instance = new PoloniexTraderClient();
	      }
	      
	      return instance;
	}

	/**
	 * 
	 */
	private PoloniexTraderClient()
	{
		propertiesUtil = PropertiesUtil.getInstance();
		
		this.key = propertiesUtil.getProps().getProperty("poloniex.api.key");
		this.secret = propertiesUtil.getProps().getProperty("poloniex.api.secret");
		
	}
	
	/**
	 * 
	 * @param key
	 * @param secret
	 * @param currencyPair
	 * @param buyPrice
	 */
	public void buy( String currencyPair, BigDecimal buyPrice)
	{
		
		PoloniexExchangeService service = new PoloniexExchangeService(this.key, this.secret);

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
