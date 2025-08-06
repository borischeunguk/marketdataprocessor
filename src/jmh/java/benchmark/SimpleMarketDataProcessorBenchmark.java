package benchmark;

import org.openjdk.jmh.annotations.*;
import standard.SimpleMarketDataProcessor;
import utils.MarketData;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for comparing the throughput of a native MarketDataProcessor
 * against a Disruptor-based implementation.
 * This benchmark focuses on measuring the throughput, latency, and
 * memory usage of the native processor.
 */
@State(Scope.Thread)
public class SimpleMarketDataProcessorBenchmark {

    private SimpleMarketDataProcessor nativeProcessor;
    private final String[] symbols = {"AAPL", "BTC", "ETH"};

    @Setup
    public void setup() {
        nativeProcessor = new SimpleMarketDataProcessor();
    }

    private MarketData randomMarketData() {
        String symbol = symbols[ThreadLocalRandom.current().nextInt(symbols.length)];
        double price = ThreadLocalRandom.current().nextDouble(100.0, 200.0);
        long timestamp = System.currentTimeMillis();
        return new MarketData(symbol, price, timestamp);
    }

    @Benchmark
    @BenchmarkMode(Mode.Throughput)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void nativeThroughput() {
        nativeProcessor.onMessage(randomMarketData());
    }

    @Benchmark
    @BenchmarkMode(Mode.SampleTime)
    @OutputTimeUnit(TimeUnit.MICROSECONDS)
    public void nativeLatency() {
        nativeProcessor.onMessage(randomMarketData());
    }
}


