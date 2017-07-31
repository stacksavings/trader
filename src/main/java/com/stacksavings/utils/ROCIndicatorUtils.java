package com.stacksavings.utils;

import java.util.ArrayList;
import java.util.List;
import eu.verdelhan.ta4j.Decimal;

/**
 * 
 * @author jpcol
 *
 */
public class ROCIndicatorUtils {

   
	/**
	 * 
	 * @param result
	 */
   public static List<Decimal> calculateRisePrice(List<Decimal> result)
    {
	   List<Decimal> resultFinal = new ArrayList<Decimal>();
	   
    	int i = 0;
    	while( i < result.size()){
    		Decimal number = result.get(i);
    		if(number.isPositive()){
    			// Check if there are 3 times positives
    			i++;
    			if(i < result.size() && result.get(i).isPositive()){
    				i++;
    				if(i < result.size() && result.get(i).isPositive()){
    					resultFinal.add(result.get(i));
    				}
    			}
    		}
    		i++;
    	}
    	return resultFinal;
    }	   
}
