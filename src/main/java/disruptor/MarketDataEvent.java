package disruptor;

import utils.MarketData;

public class MarketDataEvent {
    public MarketData data;

    public void set(MarketData data) {
        this.data = data;
    }
}

