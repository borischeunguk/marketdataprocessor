package disruptor;

import com.lmax.disruptor.EventFactory;

public class MarketDataEventFactory implements EventFactory<MarketDataEvent> {
    @Override
    public MarketDataEvent newInstance() {
        return new MarketDataEvent();
    }
}

