package com.stacksavings.utils;

import com.stacksavings.Parameter.Parameters;
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
    private Logger loggerParameters;

    private String runIdentifier;

    private final String TAB = "    ";

    public LoggerHelper() {
        logger = LogManager.getLogger("Rolling");
        loggerSummary = LogManager.getLogger("SummaryRollingLog");
        loggerCurrencySummary = LogManager.getLogger("CurrencySummaryRollingLog");
        loggerTick = LogManager.getLogger("TickRollingLog");
        loggertickCombinedSummary = LogManager.getLogger("TickCombinedSummaryRollingLog");
        loggerParameters = LogManager.getLogger("ParametersRollingLog");

        runIdentifier = generateRandomString();

        final Date date = new Date();
        final String startMessage = "******* Logging started for run: " + date + " " + runIdentifier;

        logger.info(startMessage);
        loggerSummary.info(startMessage);
        loggerCurrencySummary.info(startMessage);
        loggerTick.info(startMessage);
        loggertickCombinedSummary.info(startMessage);


        //Write Headers
        logTickRow("CURRENCY", "ACTION", "INDEX","PRICE", "AMOUNT");
        logTickCombinedSummaryRow("INDEX", "ACTIVE_POSITIONS");
        logCurrencySummaryRow("CURRENCY", "TOTAL_PROFIT", "START_FUNDS", "END_FUNDS", "PERCENT_CHANGE");
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

    public void logCurrencySummaryRow(final Object currency, final Object totalProfit, final Object startFunds, final Object endFunds, final Object percentChange) {
        final String logString = currency + TAB + totalProfit + TAB + startFunds + TAB + endFunds + TAB + percentChange;
        loggerCurrencySummary.info(logString);
    }

    public void logParameters(final Parameters parameters) {

        final boolean usesCurrencySkipList = (parameters.getCurrencySkipList() == null || parameters.getCurrencySkipList().isEmpty()) ? false : true;

        final String headersString = "RUN_ID" + TAB
                + "FROM_DATE" + TAB
                + "TO_DATE" + TAB
                + "CONVERSION_CUR" + TAB
                + "START_CUR_AMT" + TAB
                + "USE_CONVERSION" + TAB
                + "PROCESS_STOP_LOSS" + TAB
                + "FEE_AMT" + TAB
                + "STOP_LOSS_RATIO" + TAB
                + "APPLY_EXP_INDICATOR" + TAB
                + "USE_CUR_SKIP_LIST" + TAB
                + "STRATEGY" + TAB
                + "USE_CACHED_BUY_SELL";

        final String logString = runIdentifier + TAB
                + parameters.getFromDate() + TAB
                + parameters.getToDate() + TAB
                +  parameters.getConversionCurrency() + TAB
                + parameters.getInitialCurrencyAmount() + TAB
                + parameters.isUseConversionSeries() + TAB
                + parameters.shouldProccessStopLoss() + TAB
                + parameters.getFeeAmount() + TAB
                + parameters.getStopLossRatio() + TAB
                + parameters.isApplyExperimentalIndicator() + TAB
                + usesCurrencySkipList + TAB
                + parameters.getStrategyHolder().getStrategyName() + TAB
                + parameters.isUseCachedBuySellSignals();

        loggerParameters.info(headersString);
        loggerParameters.info(logString);

    }


    private String generateRandomString() {
        return UUID.randomUUID().toString();
    }


}
