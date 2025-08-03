package disruptor;

import utils.MarketData;

@FunctionalInterface
public interface MarketDataConsumer {
    void publish(MarketData data);
}

