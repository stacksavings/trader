package com.stacksavings.tradingrecord.holders;

import java.util.List;
import java.util.Map;

public class TradingRecordHolderCacheEnabled extends TradingRecordHolder {

    private List<Map<String, Boolean>> buySellCache;


    @Override
    protected boolean runEnterStrategy() {

        boolean shouldEnter = buySellCache.get(curIter).get("shouldenter");

        return shouldEnter;

    }

    @Override
    protected boolean runExitStrategy() {

       boolean shouldExit = buySellCache.get(curIter).get("shouldexit");

       return shouldExit;

    }

    public void setBuySellCache(List<Map<String, Boolean>> buySellCache) {
        this.buySellCache = buySellCache;
    }
}
