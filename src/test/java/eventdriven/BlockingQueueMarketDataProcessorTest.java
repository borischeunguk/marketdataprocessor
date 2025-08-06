package eventdriven;

import org.junit.jupiter.api.*;
import utils.MarketData;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BlockingQueueMarketDataProcessorTest {
    private BlockingQueueMarketDataProcessor processor;
    private List<MarketData> publishedData;

    @BeforeEach
    void setUp() {
        publishedData = new CopyOnWriteArrayList<>();

        processor = new BlockingQueueMarketDataProcessor() {
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
    void testSingleSymbolThrottling() throws InterruptedException {
        String symbol = "AAPL";
        for (int i = 0; i < 5; i++) {
            processor.onMessage(new MarketData(symbol, 100 + i, System.currentTimeMillis()));
            Thread.sleep(100);
        }

        TimeUnit.SECONDS.sleep(2); // give time for cooldown + retries

        long count = publishedData.stream()
                .filter(md -> symbol.equals(md.getSymbol()))
                .count();

        assertTrue(count <= 2, "Should publish at most once per second per symbol");
    }

    @Test
    void testGlobalThrottleLimit() throws InterruptedException {
        for (int i = 0; i < 500; i++) {
            processor.onMessage(new MarketData("SYM" + i, i, System.currentTimeMillis()));
        }

        TimeUnit.SECONDS.sleep(3); // should publish up to 300 max

        assertTrue(publishedData.size() <= 300, "Should not exceed global 100/sec limit");
    }

    @Test
    void testLatestDataIsPublished() throws InterruptedException {
        String symbol = "ETH";

        processor.onMessage(new MarketData(symbol, 1000, System.currentTimeMillis()));
        Thread.sleep(200);
        processor.onMessage(new MarketData(symbol, 1055, System.currentTimeMillis()));
        Thread.sleep(1200);

        boolean found = publishedData.stream()
                .anyMatch(md -> symbol.equals(md.getSymbol()) && md.getPrice() == 1055);

        assertTrue(found, "Should publish the latest update for the symbol");
    }

    @Test
    void testEventualPublishForAllSymbols() throws InterruptedException {
        for (int i = 0; i < 150; i++) {
            processor.onMessage(new MarketData("SYM" + i, i, System.currentTimeMillis()));
        }

        TimeUnit.SECONDS.sleep(3); // enough time for 100/sec throttle

        assertTrue(publishedData.size() >= 140, "Should eventually publish most or all data");
    }

    @Test
    void testNoDuplicateSymbolWithinOneSecond() throws InterruptedException {
        String symbol = "BTC";

        processor.onMessage(new MarketData(symbol, 500, System.currentTimeMillis()));
        Thread.sleep(100);
        processor.onMessage(new MarketData(symbol, 505, System.currentTimeMillis()));
        Thread.sleep(200);
        processor.onMessage(new MarketData(symbol, 510, System.currentTimeMillis()));
        Thread.sleep(1200); // total time > 1s

        long count = publishedData.stream()
                .filter(md -> symbol.equals(md.getSymbol()))
                .count();

        assertTrue(count <= 2, "Should not publish more than once per second per symbol");
    }
}

