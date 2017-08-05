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
    public void consumeDataFromApi() 
    {
    	PoloniexClientApi.getInstance().execute("USDT_BTC");
    }

}
