package com.stacksavings.indicators;

import java.util.ArrayList;
import java.util.List;

import com.stacksavings.client.api.PoloniexClientApi;
import com.stacksavings.client.api.dto.ROCIndicatorDto;
import com.stacksavings.loaders.CsvTicksLoader;
import com.stacksavings.utils.Constants;
import com.stacksavings.utils.FileManager;
import com.stacksavings.utils.ROCIndicatorUtils;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;
import eu.verdelhan.ta4j.indicators.trackers.ROCIndicator;

/**
 *
 * @author jpcol
 *
 */
public class ROCIndicatorCalculate {

	private static ROCIndicatorCalculate instance;
	private CsvTicksLoader csvTicksLoader;
	private PoloniexClientApi poloniexClientApi;
	private FileManager fileManager;

	/**
	 *
	 * @return
	 */
	public static ROCIndicatorCalculate getInstance()
	{
		if(instance == null)
		{
			instance = new ROCIndicatorCalculate();
		}

		return instance;
	}

	/**
	 *
	 */
	private ROCIndicatorCalculate()
	{
		csvTicksLoader = CsvTicksLoader.getInstance();
		poloniexClientApi = PoloniexClientApi.getInstance();
		fileManager = FileManager.getInstance();
	}


	/**
	 * calculateROC
	 */
	public void calculateROC()
	{
		List<String> currencyPairList = poloniexClientApi.returnCurrencyPair();

		if(currencyPairList != null && currencyPairList.size() > 0)
		{
			for (String currency : currencyPairList)
			{
				String fileNameCurrencyPair = fileManager.getFileNameByCurrency(currency);

				TimeSeries series = csvTicksLoader.loadSeriesByFileName(fileNameCurrencyPair);
				ClosePriceIndicator closePrice = new ClosePriceIndicator(series);

				ROCIndicator roc = new ROCIndicator(closePrice, Constants.TIME_FRAME_12);

				final int nbTicks = series.getTickCount();

				List<Decimal> results =new ArrayList<Decimal>();
				List<ROCIndicatorDto> resultROC = new ArrayList<ROCIndicatorDto>();

				for (int i = 0; i < nbTicks; i++)
				{
					results.add(roc.getValue(i));

					resultROC.add(new ROCIndicatorDto(series.getTick(i), roc.getValue(i)));
				}

				List<ROCIndicatorDto> resultFinal =  ROCIndicatorUtils.calculateRisePriceDto(resultROC);
				System.out.println("****  BUY Signal for currency : "+currency);
				for (ROCIndicatorDto rocIndicatorDto : resultFinal)
				{
					System.out.println("BeginTime: "+rocIndicatorDto.getTick().getEndTime()+ " Decimal: "+rocIndicatorDto.getDecimal()+" Price: "+rocIndicatorDto.getTick().getClosePrice());
				}

			}
		}
		else
		{
			System.out.println("No hay datos del servicio web");
		}

	}

	public static void main(String[] args) {

		ROCIndicatorCalculate.getInstance().calculateROC();

	}
}
