package standard;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.MarketData;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DequeMarketDataProcessorTest {

    private DequeMarketDataProcessor processor;
    private List<MarketData> publishedData;

    @BeforeEach
    void setUp() {
        publishedData = new CopyOnWriteArrayList<>();

        // Anonymous subclass to override publish method for testing
        processor = new DequeMarketDataProcessor() {
            @Override
            public void publishAggregatedMarketData(MarketData data) {
                publishedData.add(data);
            }
        };
    }

    @AfterEach
    void tearDown() {
        processor.shutdown();
    }

    @Test
    void testSingleSymbolPublishOncePerSecond() throws InterruptedException {
        String symbol = "AAPL";
        long now = System.currentTimeMillis();

        // Rapid updates to same symbol
        for (int i = 0; i < 10; i++) {
            processor.onMessage(new MarketData(symbol, 100.0 + i, now + i * 10));
            Thread.sleep(50);
        }

        // Wait for 2 seconds to allow potential throttled publications
        TimeUnit.SECONDS.sleep(2);

        long count = publishedData.stream().filter(md -> symbol.equals(md.getSymbol())).count();

        assertTrue(count <= 2, "Should publish at most once per second for a symbol");
    }

    @Test
    void testGlobalThrottleLimit() throws InterruptedException {
        long now = System.currentTimeMillis();

        // Send 500 unique symbols rapidly
        for (int i = 0; i < 500; i++) {
            String symbol = "SYM" + i;
            processor.onMessage(new MarketData(symbol, i, now));
        }

        // Wait for 2 seconds
        TimeUnit.SECONDS.sleep(2);

        // Should publish up to 200 messages in 2 seconds
        assertTrue(publishedData.size() <= 200, "Should not exceed global throttle limit");
    }

    @Test
    void testLatestDataIsPublished() throws InterruptedException {
        String symbol = "BTC";

        processor.onMessage(new MarketData(symbol, 100.0, System.currentTimeMillis()));
        Thread.sleep(200);
        processor.onMessage(new MarketData(symbol, 105.5, System.currentTimeMillis()));
        Thread.sleep(1200); // Enough time for at least one publish

        boolean latestPublished = publishedData.stream()
                .anyMatch(md -> symbol.equals(md.getSymbol()) && md.getPrice() == 105.5);

        assertTrue(latestPublished, "Should publish the latest data for the symbol");
    }

    @Test
    void testEventualPublishAllSymbols() throws InterruptedException {
        for (int i = 0; i < 150; i++) {
            processor.onMessage(new MarketData("SYM" + i, i, System.currentTimeMillis()));
        }

        // Wait long enough to allow them all to be published at 100/sec
        TimeUnit.SECONDS.sleep(3);

        assertTrue(publishedData.size() >= 140, "Should eventually publish most or all data");
    }

    @Test
    void testNoDuplicateSymbolPublishWithinInterval() throws InterruptedException {
        String symbol = "ETH";
        long now = System.currentTimeMillis();

        processor.onMessage(new MarketData(symbol, 2000, now));
        Thread.sleep(200); // give time for processing
        processor.onMessage(new MarketData(symbol, 2001, now + 300));
        Thread.sleep(200);
        processor.onMessage(new MarketData(symbol, 2002, now + 600));
        Thread.sleep(600); // total ~1.2s to allow full window

        long count = publishedData.stream()
                .filter(md -> symbol.equals(md.getSymbol()))
                .count();

        assertTrue(count <= 1, "Should publish only once per second per symbol");
    }

}

