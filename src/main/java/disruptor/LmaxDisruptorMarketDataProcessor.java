package disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.RingBuffer;
import utils.MarketData;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class LmaxDisruptorMarketDataProcessor {

    private final Disruptor<MarketDataEvent> disruptor;
    private final RingBuffer<MarketDataEvent> ringBuffer;

    public LmaxDisruptorMarketDataProcessor() {
        ThreadFactory threadFactory = Executors.defaultThreadFactory();

        this.disruptor = new Disruptor<>(
                new MarketDataEventFactory(),
                1024, // RingBuffer size
                threadFactory,
                ProducerType.SINGLE,
                new BlockingWaitStrategy()
        );

        this.disruptor.handleEventsWith(new MarketDataEventHandler(this::publishAggregatedMarketData));
        this.disruptor.start();
        this.ringBuffer = disruptor.getRingBuffer();
    }

    public void onMessage(MarketData data) {
        long sequence = ringBuffer.next();
        try {
            MarketDataEvent event = ringBuffer.get(sequence);
            event.set(data);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public void publishAggregatedMarketData(MarketData data) {
        // You may override this in test or benchmark
        System.out.printf("Published: %s -> %.2f at %d%n", data.getSymbol(), data.getPrice(), System.currentTimeMillis());
    }

    public void shutdown() {
        disruptor.shutdown();
    }
}
