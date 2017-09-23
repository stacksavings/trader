package com.stacksavings.utils;

import java.math.BigDecimal;
import java.util.List;

import com.cf.client.poloniex.PoloniexExchangeService;
import com.cf.data.model.poloniex.PoloniexOpenOrder;
import com.cf.data.model.poloniex.PoloniexOrderResult;
import com.cf.data.model.poloniex.PoloniexTradeHistory;
import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.TradingRecord;

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

	private PoloniexExchangeService service;
		
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

		this.service = new PoloniexExchangeService(this.key, this.secret);
		
	}
	
	/**
	 *
	 * @param currencyPair
	 * @param buyPrice
	 */
	public void buy(final String currencyPair, final BigDecimal buyPrice, final BigDecimal amount)
	{
		//temporary code
		final BigDecimal adjustedBuyPrice = buyPrice.add(buyPrice.multiply(BigDecimal.valueOf(0.0025)));

		boolean fillOrKill = false;
		boolean immediateOrCancel = false;
		boolean postOnly = false;
		PoloniexOrderResult buyOrderResult = 
		      service.buy(currencyPair, adjustedBuyPrice, amount, fillOrKill, immediateOrCancel, postOnly);
		
		if(buyOrderResult.resultingTrades != null && 
				buyOrderResult.resultingTrades.size()> 0)
		{
			for (PoloniexTradeHistory poloniexTradeHistory : buyOrderResult.resultingTrades) 
			{
				System.out.println("Poloniex Buy: " + poloniexTradeHistory.amount);
			}
		}
	}

	public void sell(final String currencyPair, final BigDecimal sellPrice, final BigDecimal amount)
	{
		//temporary code
		final BigDecimal adjustedSellPrice = sellPrice.subtract(sellPrice.multiply(BigDecimal.valueOf(0.01)));

		boolean fillOrKill = false;
		boolean immediateOrCancel = false;
		boolean postOnly = false;
		PoloniexOrderResult sellOrderResult =
				service.sell(currencyPair, adjustedSellPrice, amount, fillOrKill, immediateOrCancel, postOnly);

		if(sellOrderResult.resultingTrades != null &&
				sellOrderResult.resultingTrades.size()> 0)
		{
			for (PoloniexTradeHistory poloniexTradeHistory : sellOrderResult.resultingTrades)
			{
				System.out.println("Poloniex Sell: " + poloniexTradeHistory.amount);
			}
		}
	}

	public boolean areOpenPoloniexOrders (final String currencyPair, final TradingRecord tradingRecord)
	{

		final List<PoloniexOpenOrder>  openOrders = service.returnOpenOrders(currencyPair);

		if (openOrders != null && openOrders.size() > 0) {
			return true;
		}

		return false;
/*		for (final PoloniexOpenOrder openOrder : openOrders) {

			//TODO I think this is a complete hack, I don't fully understand if there is a way to express this in a trading record, basically we want to just show that we have an open order
			// to block any new trading activity
			tradingRecord.enter(0, Decimal.valueOf(openOrder.rate.longValue()), Decimal.valueOf(openOrder.amount.longValue()));
			break;
		}*/
	}

	public void createTradingRecordFromPoloniexTrade(final String currencyPair, final TradingRecord tradingRecord) {

		final List<PoloniexOpenOrder>  openOrders = service.returnOpenOrders(currencyPair);

		//we should not be creating a new order if there are existing orders still open
		if (openOrders.size() < 1) {
			final List<PoloniexTradeHistory> tradeHistories = service.returnTradeHistory(currencyPair);

			final PoloniexTradeHistory mostRecentTrade = tradeHistories.get(0);
			if (mostRecentTrade.type.equalsIgnoreCase("buy")) {
				//there is an open order, so we record it
				//TODO fix this later to be more accurate by using time series to determine index
				tradingRecord.enter(0, Decimal.valueOf(mostRecentTrade.rate.longValue()), Decimal.valueOf(mostRecentTrade.amount.longValue()));
				//trade left open to indicate we still hold a position in this currency
			}

			//find the most recent sell to record this
			for (final PoloniexTradeHistory tradeHistory : tradeHistories) {

				//Get the most recent sell trade
				//TODO, this could have issues, as I think it may be possible that there could end up being multiple sell trades for one initial sell order, if the prices is moving and
				//it ends up creating multiple trades to fill that one order at different prices, may need to look at ways to address this
				if (tradeHistory.type.equalsIgnoreCase("sell")) {
					//TODO fix this later to be more accurate by using time series to determine index
					tradingRecord.enter(1, Decimal.valueOf(tradeHistory.rate.longValue()), Decimal.valueOf(tradeHistory.amount.longValue()));
					tradingRecord.exit(1);
					break;
				}
			}
		}
	}

}
