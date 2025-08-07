package benchmark;

import eventdriven.BlockingQueueMarketDataProcessor;
import org.openjdk.jmh.annotations.*;
import utils.MarketData;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@BenchmarkMode({Mode.Throughput, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(1) // Only 1 fork
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS) // ~20s warmup
@Measurement(iterations = 3, time = 30, timeUnit = TimeUnit.SECONDS) // ~90s measurement
public class BlockingQueueMarketDataProcessorBenchmark {

    private BlockingQueueMarketDataProcessor processor;
    private final String[] symbols = {"AAPL", "BTC", "ETH"};

    @Setup(Level.Iteration)
    public void setup() {
        processor = new BlockingQueueMarketDataProcessor();
    }

    private MarketData randomMarketData() {
        String symbol = symbols[ThreadLocalRandom.current().nextInt(symbols.length)];
        double price = ThreadLocalRandom.current().nextDouble(100.0, 200.0);
        long timestamp = System.currentTimeMillis();
        return new MarketData(symbol, price, timestamp);
    }

    @Benchmark
    public void throughput() {
        processor.onMessage(randomMarketData());
    }

    @Benchmark
    public void latency() {
        processor.onMessage(randomMarketData());
    }

    @TearDown(Level.Iteration)
    public void tearDown() {
        processor.shutdown();
    }
}
