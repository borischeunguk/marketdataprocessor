package eventdriven;

import utils.MarketData;

import java.util.Deque;
import java.util.concurrent.*;

public class EventBasedMarketDataProcessor {

    private final BlockingQueue<MarketData> queue = new LinkedBlockingQueue<>();
    private final ConcurrentHashMap<String, MarketData> latestBySymbol = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> lastPublished = new ConcurrentHashMap<>();
    private final Deque<Long> publishTimestamps = new ConcurrentLinkedDeque<>();
    private final ScheduledExecutorService flushScheduler = Executors.newSingleThreadScheduledExecutor();
    private final Thread worker;
    private volatile boolean running = true;

    public EventBasedMarketDataProcessor() {
        worker = new Thread(this::runWorker);
        worker.start();

        // New: schedule periodic flush of pending coalesced updates
        flushScheduler.scheduleAtFixedRate(this::processReadySymbols, 100, 100, TimeUnit.MILLISECONDS);
    }

    public void onMessage(MarketData data) {
        queue.offer(data); // triggers the worker to react
    }

    private void runWorker() {
        while (running) {
            try {
                MarketData data = queue.take(); // blocks until new data arrives
                latestBySymbol.put(data.getSymbol(), data);

                processReadySymbols();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void processReadySymbols() {
        long now = System.currentTimeMillis();

        // Clean up expired timestamps
        while (!publishTimestamps.isEmpty() && now - publishTimestamps.peekFirst() > 1000) {
            publishTimestamps.pollFirst();
        }

        int remainingQuota = 100 - publishTimestamps.size();
        if (remainingQuota <= 0) return;

        int published = 0;
        for (String symbol : latestBySymbol.keySet()) {
            if (published >= remainingQuota) break;

            long last = lastPublished.getOrDefault(symbol, 0L);
            if (now - last < 1000) continue;

            MarketData data = latestBySymbol.remove(symbol);
            if (data != null) {
                publishAggregatedMarketData(data);
                lastPublished.put(symbol, now);
                publishTimestamps.addLast(now);
                published++;
            }
        }
    }

    public void publishAggregatedMarketData(MarketData data) {
        System.out.printf("Published: %s -> %.2f at %d%n", data.getSymbol(), data.getPrice(), System.currentTimeMillis());
    }

    public void shutdown() {
        running = false;
        worker.interrupt();
        flushScheduler.shutdown();
    }
}
