package com.stacksavings.utils;

import eu.verdelhan.ta4j.Order;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Date;
import java.util.UUID;

public class LoggerHelper {

    private Logger logger;
    private Logger loggerSummary;
    private Logger loggerCurrencySummary;
    private Logger loggerTick;
    private Logger loggertickCombinedSummary;

    private final String TAB = "    ";

    public LoggerHelper() {
        logger = LogManager.getLogger("Rolling");
        loggerSummary = LogManager.getLogger("SummaryRollingLog");
        loggerCurrencySummary = LogManager.getLogger("CurrencySummaryRollingLog");
        loggerTick = LogManager.getLogger("TickRollingLog");
        loggertickCombinedSummary = LogManager.getLogger("TickCombinedSummaryRollingLog");


        final Date date = new Date();
        final String startMessage = "******* Logging started for run: " + date + " " + generateRandomString();

        logger.info(startMessage);
        loggerSummary.info(startMessage);
        loggerCurrencySummary.info(startMessage);
        loggerTick.info(startMessage);
        loggertickCombinedSummary.info(startMessage);


        //Write Headers
        logTickRow("CURRENCY", "ACTION", "INDEX","PRICE", "AMOUNT");
        logTickCombinedSummaryRow("INDEX", "ACTIVE_POSITIONS");
        logCurrencySummaryRow("TOTAL_PROFIT", "START_FUNDS", "END_FUNDS", "PERCENT_CHANGE");
        logSummaryRow("TOTAL_START", "TOTAL_END", "PERCENT_CHANGE");

    }

    public Logger getDefaultLogger() {
        return logger;
    }



    public void logTickRow(final Object currency, final Object action, final Object index,  final Object price, final Object amount) {
        final String logString = currency + TAB + action + TAB + index + TAB + price + TAB + amount;
        loggerTick.info(logString);
    }

    public void logTickCombinedSummaryRow(final Object index, final Object activePositions) {
        final String logString = index + TAB + activePositions;
        loggertickCombinedSummary.info(logString);
    }

    public void logSummaryRow(final Object totalStart, final Object totalEnd, final Object percentChange) {
        final String logString = totalStart + TAB + totalEnd + TAB + percentChange;
        loggerSummary.info(logString);
    }

    public void logCurrencySummaryRow(final Object totalProfit, final Object startFunds, final Object endFunds, final Object percentChange) {
        final String logString = totalProfit + TAB + startFunds + TAB + endFunds + TAB + percentChange;
        loggerCurrencySummary.info(logString);
    }


    private String generateRandomString() {
        return UUID.randomUUID().toString();
    }


}
