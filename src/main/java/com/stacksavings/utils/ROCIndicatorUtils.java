package com.stacksavings.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.stacksavings.client.api.dto.ROCIndicatorDto;

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

	/**
	 *
	 * @param result
	 * @return
	 */
	public static List<ROCIndicatorDto> calculateRisePriceDto(List<ROCIndicatorDto> result)
	{
		List<ROCIndicatorDto> resultFinal = new ArrayList<ROCIndicatorDto>();

		int i = 0;
		while( i < result.size())
		{
			ROCIndicatorDto number = result.get(i);
			if(number.getDecimal().isPositive())
			{
				// Check if there are 3 times positives
				i++;
				if(i < result.size() && result.get(i).getDecimal().isPositive())
				{
					i++;
					if(i < result.size() && result.get(i).getDecimal().isPositive())
					{
						resultFinal.add(result.get(i));
					}
				}
			}
			i++;
		}

		Collections.sort(resultFinal);

		if(resultFinal.size()>=5){
			resultFinal = resultFinal.subList(0, 4);
		}

		return resultFinal;
	}



}
