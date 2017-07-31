package com.stacksavings;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.stacksavings.loaders.CsvTicksLoader;
import com.stacksavings.utils.ROCIndicatorUtils;

import eu.verdelhan.ta4j.Decimal;
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
        
        List<Decimal> results =new ArrayList<Decimal>();
        
        for (int i = 0; i < nbTicks; i++) {
        	System.out.println( roc.getValue(i) );
        	results.add(roc.getValue(i)) ;
        }
        
        List<Decimal> resultFinal =  ROCIndicatorUtils.calculateRisePrice(results);
        for (Decimal decimal : resultFinal) {
			System.out.println(" BUY Signal !!! "+decimal);
		}
        
    }
    
 
}
