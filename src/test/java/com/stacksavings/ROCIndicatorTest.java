package com.stacksavings;

import static eu.verdelhan.ta4j.TATestsUtils.assertDecimalEquals;

import org.junit.Before;
import org.junit.Test;

import com.stacksavings.loaders.CsvTicksLoader;

import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.ROCIndicator;

/**
 * 
 * @author julio paulo
 *
 */
public class ROCIndicatorTest {

    private ClosePriceIndicator closePrice;

    private TimeSeries series;
    
    @Before
    public void setUp() {
    	
    	series = CsvTicksLoader.getInstance().loadSeriesByFileName("src//test//resources//files//chart_data_ROC.csv");
    	
        closePrice = new ClosePriceIndicator(series);
    }

    @Test
    public void getValueWhenTimeFrameIs12() {
    	
        ROCIndicator roc = new ROCIndicator(closePrice, 12);

        final int nbTicks = series.getTickCount();
        
        for (int i = 0; i < nbTicks; i++) {
        	System.out.println( roc.getValue(i) );
        }
        
    }
}
