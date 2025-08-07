package disruptor;

import com.lmax.disruptor.EventHandler;
import utils.MarketData;

import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.*;

/**
 * MarketDataEventHandler processes market data events with rate limiting
 * and ensures the latest data is published
 * while adhering to global and per-symbol constraints.
 *
 */
public class MarketDataEventHandler implements EventHandler<MarketDataEvent> {

    private final Map<String, Long> lastPublishedPerSymbol = new ConcurrentHashMap<>();
    private final Deque<Long> publishTimestamps = new ConcurrentLinkedDeque<>();
    private final Map<String, MarketData> latestDataBySymbol = new ConcurrentHashMap<>();
    private final Set<String> retrySymbols = ConcurrentHashMap.newKeySet();

    private static final int MAX_GLOBAL_PUBLISHES_PER_SEC = 100;
    private static final long SYMBOL_COOLDOWN_MS = 1000;
    private static final long GLOBAL_WINDOW_MS = 1000;

    private final MarketDataConsumer publisher;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public MarketDataEventHandler(MarketDataConsumer publisher) {
        this.publisher = publisher;
        scheduler.scheduleAtFixedRate(this::flushRetries, 50, 50, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onEvent(MarketDataEvent event, long sequence, boolean endOfBatch) {
        MarketData data = event.data;
        if (data == null) return;

        String symbol = data.getSymbol();
        latestDataBySymbol.put(symbol, data); // Always keep the latest

        if (!tryPublish(symbol)) {
            retrySymbols.add(symbol);
        }
    }

    private boolean tryPublish(String symbol) {
        long now = System.currentTimeMillis();

        // Enforce global rate limit window
        while (!publishTimestamps.isEmpty() && now - publishTimestamps.peekFirst() > GLOBAL_WINDOW_MS) {
            publishTimestamps.pollFirst();
        }

        if (publishTimestamps.size() >= MAX_GLOBAL_PUBLISHES_PER_SEC) {
            return false; // Global throttle
        }

        Long lastPublished = lastPublishedPerSymbol.getOrDefault(symbol, 0L);
        if (now - lastPublished < SYMBOL_COOLDOWN_MS) {
            return false; // Per-symbol cooldown
        }

        MarketData latest = latestDataBySymbol.get(symbol);
        if (latest == null) return true; // No data to publish

        // ✅ Publish and update state
        publisher.publish(latest);
        lastPublishedPerSymbol.put(symbol, now);
        publishTimestamps.addLast(now);

        latestDataBySymbol.remove(symbol);
        retrySymbols.remove(symbol);

        return true;
    }

    private void flushRetries() {
        for (String symbol : new HashSet<>(retrySymbols)) {
            tryPublish(symbol);
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
