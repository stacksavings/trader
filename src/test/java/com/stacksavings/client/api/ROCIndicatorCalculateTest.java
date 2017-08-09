package com.stacksavings.client.api;

import org.junit.Before;
import org.junit.Test;

import com.stacksavings.indicators.ROCIndicatorCalculate;

public class ROCIndicatorCalculateTest {

	
	@Before
    public void setUp() {
    	
    }
    
    @Test
    public void calculateROC()
    {
    	 ROCIndicatorCalculate.getInstance().calculateROC();
    }
    
}
