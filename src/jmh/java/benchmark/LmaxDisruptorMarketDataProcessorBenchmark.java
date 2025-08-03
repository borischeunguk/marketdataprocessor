package benchmark;

import disruptor.LmaxDisruptorMarketDataProcessor;
import org.openjdk.jmh.annotations.*;
import utils.MarketData;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Thread)
public class LmaxDisruptorMarketDataProcessorBenchmark {

    private LmaxDisruptorMarketDataProcessor processor;
    private static final AtomicLong counter = new AtomicLong();

    @Setup(Level.Iteration)
    public void setup() {
        processor = new LmaxDisruptorMarketDataProcessor(data -> {
            // No-op for benchmarking
        });
    }

    @TearDown(Level.Iteration)
    public void teardown() {
        processor.shutdown();
    }

    @Benchmark
    public void disruptorThroughput() {
        long id = counter.incrementAndGet();
        processor.onMessage(new MarketData("SYM" + (id % 100), id, System.currentTimeMillis()));
    }
}
