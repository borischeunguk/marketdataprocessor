package standard;

import utils.MarketData;

import java.util.Map;
import java.util.Deque;
import java.util.concurrent.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

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

    // Receive incoming market data
    public void onMessage(MarketData data) {
        latestBySymbol.put(data.getSymbol(), data);
    }

    // Periodically called by scheduler
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
