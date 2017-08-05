package com.stacksavings.client.api;

import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author julio paulo
 *
 */
public class PoloniexClientApiTest {
	
    @Before
    public void setUp() {
    	
    }
    
    @Test
    public void consumeDataFromApiUSDT_BTC() 
    {
    	PoloniexClientApi.getInstance().execute("USDT_BTC");
    }

    @Test
    public void consumeDataFromApiBTC_ETH() 
    {
    	PoloniexClientApi.getInstance().execute("BTC_ETH");
    }
    
    @Test
    public void consumeDataFromApiBTC_STR() 
    {
    	PoloniexClientApi.getInstance().execute("BTC_STR");
    }

    @Test
    public void consumeDataFromApiBTC_XRP() 
    {
    	PoloniexClientApi.getInstance().execute("BTC_XRP");
    }
    
    @Test
    public void consumeDataFromApiBTC_LTC() 
    {
    	PoloniexClientApi.getInstance().execute("BTC_LTC");
    }
    
    @Test
    public void consumeDataFromApiUSDT_ETH() 
    {
    	PoloniexClientApi.getInstance().execute("USDT_ETH");
    }

    @Test
    public void consumeDataFromApiBTC_BTS() 
    {
    	PoloniexClientApi.getInstance().execute("BTC_BTS");
    }
    
    @Test
    public void consumeDataFromApiBTC_NXT() 
    {
    	PoloniexClientApi.getInstance().execute("BTC_NXT");
    }
    
    @Test
    public void consumeDataFromApiBTC_DGB() 
    {
    	PoloniexClientApi.getInstance().execute("BTC_DGB");
    }
}
