package standard;

import utils.MarketData;

import java.util.Map;
import java.util.Deque;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * SimpleMarketDataProcessor is responsible for processing and publishing market data
 * while adhering to rate limits for both global and per-symbol publishing.
 *
 * Key Features:
 * - Maintains the latest market data for each symbol.
 * - Enforces a global publish rate limit (MAX_GLOBAL_RATE).
 * - Enforces a per-symbol publish interval (SYMBOL_PUBLISH_INTERVAL_MS).
 * - Uses a sliding window for global rate limiting.
 */
public class SimpleMarketDataProcessor {

    private final ConcurrentHashMap<String, MarketData> latestBySymbol = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastPublishedTime = new ConcurrentHashMap<>();
    private final Deque<Long> publishTimestamps = new ConcurrentLinkedDeque<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final int MAX_GLOBAL_RATE = 100; // max publishes/sec
    private static final long SYMBOL_PUBLISH_INTERVAL_MS = 1000; // 1/sec per symbol
    private static final long GLOBAL_WINDOW_MS = 1000; // sliding window

    public SimpleMarketDataProcessor() {
        scheduler.scheduleAtFixedRate(this::processAndPublish, 0, 10, TimeUnit.MILLISECONDS);
    }

    /**
     * Receives incoming market data and stores it in a thread-safe map.
     * The assumption is that unique symbols dont exceed the global rate limit.
     * I.E. the onMessage will NOT publish more than MAX_GLOBAL_RATE unique symbols per second.
     *
     * Time Complexity: O(1) - ConcurrentHashMap put operation is O(1) on average.
     * Space Complexity: O(n) - Where n is the number of unique symbols being tracked.
     *
     * @param data The incoming market data to process.
     */
    public void onMessage(MarketData data) {
        latestBySymbol.put(data.getSymbol(), data);
    }

    /**
     * Periodically processes and publishes market data while adhering to rate limits.
     *
     * Time Complexity: O(s) - Where s is the number of symbols in the latestBySymbol map.
     * Space Complexity: O(n) - Maintains a sliding window of timestamps and maps for symbols.
     */
    private void processAndPublish() {
        long now = System.currentTimeMillis();

        // Clean up global timestamp window
        while (!publishTimestamps.isEmpty() && now - publishTimestamps.peekFirst() > GLOBAL_WINDOW_MS) {
            publishTimestamps.pollFirst();
        }

        int remainingQuota = MAX_GLOBAL_RATE - publishTimestamps.size();
        if (remainingQuota <= 0) return;

        int publishedCount = 0;

        for (Map.Entry<String, MarketData> entry : latestBySymbol.entrySet()) {
            if (publishedCount >= remainingQuota) break;

            String symbol = entry.getKey();
            MarketData data = entry.getValue();
            Long lastTime = lastPublishedTime.getOrDefault(symbol, 0L);

            if (now - lastTime >= SYMBOL_PUBLISH_INTERVAL_MS) {
                publishAggregatedMarketData(data);
                lastPublishedTime.put(symbol, now);
                publishTimestamps.addLast(now);
                latestBySymbol.remove(symbol); // remove to avoid re-publish
                publishedCount++;
            }
        }
    }

    // Publish aggregated and throttled market data
    public void publishAggregatedMarketData(MarketData data) {
        // Placeholder for actual downstream publishing
        System.out.printf("Published: %s -> %.2f at %d%n", data.getSymbol(), data.getPrice(), System.currentTimeMillis());
    }

    // For test or shutdown
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
