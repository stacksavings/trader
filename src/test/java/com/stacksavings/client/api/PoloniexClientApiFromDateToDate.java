package com.stacksavings.client.api;

import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author julio paulo
 *
 */
public class PoloniexClientApiFromDateToDate {

	
    @Before
    public void setUp() {
    	
    }
    
    @Test
    public void executeFromDateToDate() 
    {
    	// yyyy-MM-dd HH:mm:ss
    	String fromDate = "2017-09-15 22:00:00";
    	// yyyy-MM-dd HH:mm:ss
    	String toDate = "2017-09-16 22:00:00";
    	
    	PoloniexClientApi.getInstance().execute(fromDate, toDate);
    }

}
