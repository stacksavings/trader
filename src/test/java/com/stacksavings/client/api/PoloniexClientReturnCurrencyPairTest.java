package com.stacksavings.client.api;

import java.util.List;

import org.junit.Before;
import org.junit.Test;


/**
 * 
 * @author jpcol
 *
 */
public class PoloniexClientReturnCurrencyPairTest {

    @Before
    public void setUp() {
    	
    }
    
    @Test
    public void returnTicker(){
    	List<String> result = PoloniexClientApi.getInstance().returnCurrencyPair();
    	for (String string : result) {
			System.out.println(string);
		}
    }
}
