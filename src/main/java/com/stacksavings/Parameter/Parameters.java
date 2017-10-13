package com.stacksavings.Parameter;

import com.stacksavings.allocation.Allocator;
import com.stacksavings.strategies.StrategyHolder;
import eu.verdelhan.ta4j.Decimal;

import java.util.List;

public class Parameters {

    private String fromDate;

    private String toDate;

    private Decimal initialCurrencyAmount;

    private boolean liveTradeMode;

    private List<String> currencyIncludeList;

    private List<String> currencySkipList;

    private boolean processStopLoss;

    private Decimal stopLossRatio;

    private Decimal feeAmount;

    private String conversionCurrency;

    private StrategyHolder strategyHolder;

    private boolean useConversionSeries;

    private boolean applyExperimentalIndicator;

    private boolean useCachedBuySellSignals;

    private Allocator allocator;

    public String getFromDate() {
        return fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public boolean isLiveTradeMode() {
        return liveTradeMode;
    }

    public void setLiveTradeMode(boolean liveTradeMode) {
        this.liveTradeMode = liveTradeMode;
    }

    public List<String> getCurrencySkipList() {
        return currencySkipList;
    }

    public void setCurrencySkipList(List<String> currencySkipList) {
        this.currencySkipList = currencySkipList;
    }

    public boolean shouldProccessStopLoss() {
        return processStopLoss;
    }

    public void setProcessStopLoss(boolean processStopLoss) {
        this.processStopLoss = processStopLoss;
    }

    public Decimal getStopLossRatio() {
        return stopLossRatio;
    }

    public void setStopLossRatio(Decimal stopLossRatio) {
        this.stopLossRatio = stopLossRatio;
    }

    public String getConversionCurrency() {
        return conversionCurrency;
    }

    public void setConversionCurrency(String conversionCurrency) {
        this.conversionCurrency = conversionCurrency;
    }

    public StrategyHolder getStrategyHolder() {
        return strategyHolder;
    }

    public void setStrategyHolder(StrategyHolder strategyHolder) {
        this.strategyHolder = strategyHolder;
    }

    public Decimal getInitialCurrencyAmount() {
        return initialCurrencyAmount;
    }

    public void setInitialCurrencyAmount(Decimal initialCurrencyAmount) {
        this.initialCurrencyAmount = initialCurrencyAmount;
    }

    public boolean isUseConversionSeries() {
        return useConversionSeries;
    }

    public void setUseConversionSeries(boolean useConversionSeries) {
        this.useConversionSeries = useConversionSeries;
    }

    public Decimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(Decimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public boolean isApplyExperimentalIndicator() {
        return applyExperimentalIndicator;
    }

    public void setApplyExperimentalIndicator(boolean applyExperimentalIndicator) {
        this.applyExperimentalIndicator = applyExperimentalIndicator;
    }

    public Allocator getAllocator() {
        return allocator;
    }

    public void setAllocator(Allocator allocator) {
        this.allocator = allocator;
    }

    public List<String> getCurrencyIncludeList() {
        return currencyIncludeList;
    }

    public void setCurrencyIncludeList(List<String> currencyIncludeList) {
        this.currencyIncludeList = currencyIncludeList;
    }

    public boolean isUseCachedBuySellSignals() {
        return useCachedBuySellSignals;
    }

    public void setUseCachedBuySellSignals(boolean useCachedBuySellSignals) {
        this.useCachedBuySellSignals = useCachedBuySellSignals;
    }
}
