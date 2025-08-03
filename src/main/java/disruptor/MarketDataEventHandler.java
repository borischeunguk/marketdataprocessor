package disruptor;

import com.lmax.disruptor.EventHandler;
import utils.MarketData;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public class MarketDataEventHandler implements EventHandler<MarketDataEvent> {

    private final Map<String, Long> lastPublishedPerSymbol = new ConcurrentHashMap<>();
    private final Deque<Long> publishTimestamps = new ConcurrentLinkedDeque<>();

    private static final int MAX_GLOBAL_PUBLISHES_PER_SEC = 100;
    private static final long SYMBOL_COOLDOWN_MS = 1000;
    private static final long GLOBAL_WINDOW_MS = 1000;

    private final MarketDataConsumer publisher;

    public MarketDataEventHandler(MarketDataConsumer publisher) {
        this.publisher = publisher;
    }

    @Override
    public void onEvent(MarketDataEvent event, long sequence, boolean endOfBatch) {
        MarketData data = event.data;
        long now = System.currentTimeMillis();

        // Clean up old timestamps (global throttle)
        while (!publishTimestamps.isEmpty() && now - publishTimestamps.peekFirst() > GLOBAL_WINDOW_MS) {
            publishTimestamps.pollFirst();
        }

        if (publishTimestamps.size() >= MAX_GLOBAL_PUBLISHES_PER_SEC) {
            return; // Throttled globally
        }

        // Per-symbol throttling
        Long lastTime = lastPublishedPerSymbol.getOrDefault(data.getSymbol(), 0L);
        if (now - lastTime < SYMBOL_COOLDOWN_MS) {
            return;
        }

        // Publish
        publisher.publish(data);
        publishTimestamps.addLast(now);
        lastPublishedPerSymbol.put(data.getSymbol(), now);
    }
}

