package eventdriven;

import utils.MarketData;

import java.util.Map;
import java.util.concurrent.*;

/**
 * BlockingQueueMarketDataProcessor is responsible for processing and publishing market data
 * while adhering to rate limits for both global and per-symbol publishing.
 *
 * Uses ArrayBlockingQueue to efficiently and thread-safely track global publish timestamps.
 */
public class BlockingQueueMarketDataProcessor {

    private final Map<String, MarketData> latestBySymbol = new ConcurrentHashMap<>();
    private final Map<String, Long> lastPublishedTime = new ConcurrentHashMap<>();
    private final BlockingQueue<Long> publishTimestamps = new ArrayBlockingQueue<>(MAX_GLOBAL_RATE + 10);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private static final int MAX_GLOBAL_RATE = 100; // max publishes/sec
    private static final long SYMBOL_PUBLISH_INTERVAL_MS = 1000; // 1/sec per symbol
    private static final long GLOBAL_WINDOW_MS = 1000; // sliding window

    public BlockingQueueMarketDataProcessor() {
        scheduler.scheduleAtFixedRate(this::processAndPublish, 0, 10, TimeUnit.MILLISECONDS);
    }

    /**
     * Receives incoming market data and stores the latest value per symbol.
     *
     * @param data The incoming market data to process.
     */
    public void onMessage(MarketData data) {
        latestBySymbol.put(data.getSymbol(), data);
    }

    /**
     * Periodically processes and publishes market data while adhering to global and per-symbol rate limits.
     */
    private void processAndPublish() {
        long now = System.currentTimeMillis();

        // Clean up expired global timestamps
        while (!publishTimestamps.isEmpty()) {
            Long ts = publishTimestamps.peek();
            if (ts != null && now - ts > GLOBAL_WINDOW_MS) {
                publishTimestamps.poll();
            } else {
                break;
            }
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
                publishTimestamps.offer(now);
                latestBySymbol.remove(symbol); // avoid re-publishing
                publishedCount++;
            }
        }
    }

    /**
     * Publishes the aggregated and throttled market data.
     */
    public void publishAggregatedMarketData(MarketData data) {
        System.out.printf("Published: %s -> %.2f at %d%n", data.getSymbol(), data.getPrice(), System.currentTimeMillis());
    }

    /**
     * Clean shutdown for the scheduler.
     */
    public void shutdown() {
        scheduler.shutdownNow();
    }
}
