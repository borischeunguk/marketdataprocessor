package benchmark;

import org.openjdk.jmh.annotations.*;
import standard.MarketDataProcessor;
import utils.MarketData;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark for comparing the throughput of a native MarketDataProcessor
 * against a Disruptor-based implementation.
 * This benchmark focuses on measuring the throughput, latency, and
 * memory usage of the native processor.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class MarketDataProcessorBenchmark {

    private MarketDataProcessor nativeProcessor;
//    private DisruptorMarketDataProcessor disruptorProcessor;

    @Setup
    public void setup() {
        nativeProcessor = new MarketDataProcessor();
//        disruptorProcessor = new DisruptorMarketDataProcessor();
    }

    @Benchmark
    public void nativeThroughput() {
        nativeProcessor.onMessage(new MarketData("AAPL", 101.0, System.currentTimeMillis()));
    }

//    @Benchmark
//    public void disruptorThroughput() {
//        disruptorProcessor.onMessage(new MarketData("AAPL", 101.0, System.currentTimeMillis()));
//    }
}
