package com.stacksavings.client.api;

import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author jpcol
 *
 */
public class PoloniexClientApiTest {
	
    @Before
    public void setUp() {
    	
    }
    
    @Test
    public void consumeDataFromApi() 
    {
    	PoloniexClientApi.getInstance().execute();
    }

}
