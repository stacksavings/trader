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
        
        // Incomplete time frame
        /** assertDecimalEquals(roc.getValue(0), 0);
        assertDecimalEquals(roc.getValue(1), 1.105);
        assertDecimalEquals(roc.getValue(2), -0.3319);
        assertDecimalEquals(roc.getValue(3), 0.9648);

        // Complete time frame
        double[] results13to20 = new double[] { -3.8488, -4.8489, -4.5206, -6.3439, -7.8592, -6.2083, -4.3131, -3.2434 };
        for (int i = 0; i < results13to20.length; i++) {
            assertDecimalEquals(roc.getValue(i + 12), results13to20[i]);
        }
        **/
    }
}
