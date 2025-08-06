package benchmark;

import org.openjdk.jmh.annotations.*;
import standard.DequeMarketDataProcessor;
import utils.MarketData;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark for comparing the throughput of a native MarketDataProcessor
 * against a Disruptor-based implementation.
 * This benchmark focuses on measuring the throughput, latency, and
 * memory usage of the native processor.
 */
@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(1) // Only 1 fork
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS) // ~20s warmup
@Measurement(iterations = 3, time = 30, timeUnit = TimeUnit.SECONDS) // ~90s measurement
public class DequeMarketDataProcessorBenchmark {

    private DequeMarketDataProcessor nativeProcessor;
    private final String[] symbols = {"AAPL", "BTC", "ETH"};

    @Setup
    public void setup() {
        nativeProcessor = new DequeMarketDataProcessor();
    }

    private MarketData randomMarketData() {
        String symbol = symbols[ThreadLocalRandom.current().nextInt(symbols.length)];
        double price = ThreadLocalRandom.current().nextDouble(100.0, 200.0);
        long timestamp = System.currentTimeMillis();
        return new MarketData(symbol, price, timestamp);
    }

    @Benchmark
    public void nativeThroughput() {
        nativeProcessor.onMessage(randomMarketData());
    }

    @Benchmark
    public void nativeLatency() {
        nativeProcessor.onMessage(randomMarketData());
    }
}
