package com.stacksavings.client.api;

import org.junit.Before;
import org.junit.Test;

import com.stacksavings.indicators.ROCIndicatorCalculate;

public class PoloniexClientApiAll {

	
    @Before
    public void setUp() {
    	
    }
    
    @Test
    public void generateCSVFile() 
    {
    	PoloniexClientApi.getInstance().execute();
    	
    	ROCIndicatorCalculate.getInstance().calculateROC();
    }
}
