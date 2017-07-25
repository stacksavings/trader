package com.stacksavings.main;

import java.awt.Color;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.time.Minute;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

import com.stacksavings.loaders.CsvTicksLoader;
import com.stacksavings.strategies.BuySellStrategy;

import eu.verdelhan.ta4j.Decimal;
import eu.verdelhan.ta4j.Indicator;
import eu.verdelhan.ta4j.Strategy;
import eu.verdelhan.ta4j.Tick;
import eu.verdelhan.ta4j.TimeSeries;
import eu.verdelhan.ta4j.Trade;
import eu.verdelhan.ta4j.indicators.simple.ClosePriceIndicator;

/**
 * 
 * @author jpcol
 *
 */
public class BuySell {

    /**
     * Builds a JFreeChart time series from a Ta4j time series and an indicator.
     * @param tickSeries the ta4j time series
     * @param indicator the indicator
     * @param name the name of the chart time series
     * @return the JFreeChart time series
     */
    private static org.jfree.data.time.TimeSeries buildChartTimeSeries(TimeSeries tickSeries, Indicator<Decimal> indicator, String name) {
        org.jfree.data.time.TimeSeries chartTimeSeries = new org.jfree.data.time.TimeSeries(name);
        for (int i = 0; i < tickSeries.getTickCount(); i++) {
            Tick tick = tickSeries.getTick(i);
            //chartTimeSeries.add(new Minute(Date.from(tick.getEndTime().toInstant())), indicator.getValue(i).toDouble());
            //chartTimeSeries.addOrUpdate(new Minute(Date.from(tick.getEndTime().toInstant())), indicator.getValue(i).toDouble());
            chartTimeSeries.addOrUpdate(new Second(Date.from(tick.getEndTime().toInstant())), indicator.getValue(i).toDouble());
        }
        return chartTimeSeries;
    }
    
    /**
     * Runs a strategy over a time series and adds the value markers
     * corresponding to buy/sell signals to the plot.
     * @param series a time series
     * @param strategy a trading strategy
     * @param plot the plot
     */
    private static void addBuySellSignals(TimeSeries series, Strategy strategy, XYPlot plot) {
        // Running the strategy
        List<Trade> trades = series.run(strategy).getTrades();
        // Adding markers to plot
        for (Trade trade : trades) {
            // Buy signal
            //double buySignalTickTime = new Minute(Date.from(series.getTick(trade.getEntry().getIndex()).getEndTime().toInstant())).getFirstMillisecond();
            double buySignalTickTime = new Second(Date.from(series.getTick(trade.getEntry().getIndex()).getEndTime().toInstant())).getFirstMillisecond();
            Marker buyMarker = new ValueMarker(buySignalTickTime);
            buyMarker.setPaint(Color.GREEN);
            buyMarker.setLabel("B");
            plot.addDomainMarker(buyMarker);
            // Sell signal
            //double sellSignalTickTime = new Minute(Date.from(series.getTick(trade.getExit().getIndex()).getEndTime().toInstant())).getFirstMillisecond();
            double sellSignalTickTime = new Second(Date.from(series.getTick(trade.getExit().getIndex()).getEndTime().toInstant())).getFirstMillisecond();
            Marker sellMarker = new ValueMarker(sellSignalTickTime);
            sellMarker.setPaint(Color.RED);
            sellMarker.setLabel("S");
            plot.addDomainMarker(sellMarker);
        }
    }
    
    /**
     * Displays a chart in a frame.
     * @param chart the chart to be displayed
     */
    private static void displayChart(JFreeChart chart) {
        // Chart panel
        ChartPanel panel = new ChartPanel(chart);
        panel.setFillZoomRectangle(true);
        panel.setMouseWheelEnabled(true);
        panel.setPreferredSize(new Dimension(1024, 400));
        // Application frame
        ApplicationFrame frame = new ApplicationFrame("Ta4j example - Buy and sell signals to chart");
        frame.setContentPane(panel);
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }

    
	public static void main(String[] args) 
	{
		//1. Load time series
		TimeSeries series = CsvTicksLoader.getInstance().loadSeries();
		
		//2. Load time series
		Strategy strategy = BuySellStrategy.buildStrategy(series);
		
		
		List<Trade> trades = series.run(strategy).getTrades();
		for (Trade trade : trades) {
			
			System.out.println("Type: "+ trade.getExit().getType()+" Index:"+trade.getExit().getIndex()+" Price:"+trade.getExit().getPrice());
			System.out.println("Type: "+ trade.getEntry().getType()+" Index:"+trade.getEntry().getIndex()+" Price:"+trade.getEntry().getPrice());
		}
		//3. Graphics
		
		
		   /**
         * Building chart datasets
         */
        //TimeSeriesCollection dataset = new TimeSeriesCollection();
        //dataset.addSeries(buildChartTimeSeries(series, new ClosePriceIndicator(series), "Bitstamp Bitcoin (BTC)"));

        /**
         * Creating the chart
         */
        /** JFreeChart chart = ChartFactory.createTimeSeriesChart(
                "Bitstamp BTC", // title
                "Date", // x-axis label
                "Price", // y-axis label
                dataset, // data
                true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
                );
        XYPlot plot = (XYPlot) chart.getPlot();
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("MM-dd HH:mm"));
		**/
		
        /**
         * Running the strategy and adding the buy and sell signals to plot
         */
         //addBuySellSignals(series, strategy, plot);
        
        /**
         * Displaying the chart
         */
         //displayChart(chart);
        
       /** 
        * List<Trade> trades = series.run(strategy).getTrades();
        for (Trade trade : trades) {
        	 System.out.println( trade.getEntry().getType() );
        }
		*/
	}
}
