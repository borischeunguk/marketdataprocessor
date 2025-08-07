package disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.RingBuffer;
import utils.MarketData;

import java.util.concurrent.Executors;

/**
 * LmaxDisruptorMarketDataProcessor is a high-performance market data processor
 * that leverages the LMAX Disruptor library for low-latency and high-throughput
 * event processing.
 *
 * Key Features:
 * - Uses a ring buffer to efficiently handle market data events.
 * - Supports single-producer and multi-consumer configurations.
 * - Enforces rate limits and ensures the latest data is published.
 */
public class LmaxDisruptorMarketDataProcessor {
    private final Disruptor<MarketDataEvent> disruptor;
    /**
     * Considering the onMessage is single-threaded,
     * RingBufferUnsafe can be used for better performance.
     */
    private final RingBuffer<MarketDataEvent> ringBuffer;
    private final MarketDataEventHandler handler;

    public LmaxDisruptorMarketDataProcessor(MarketDataConsumer publisher) {
        this.handler = new MarketDataEventHandler(publisher);

        disruptor = new Disruptor<>(
                MarketDataEvent::new,
                1024,
                Executors.defaultThreadFactory(),
                ProducerType.SINGLE,
                new BlockingWaitStrategy()
        );

        disruptor.handleEventsWith(handler);
        disruptor.start();
        ringBuffer = disruptor.getRingBuffer();
    }

    /**
     * Publishes a new market data event to the ring buffer.
     *
     * Time Complexity: O(1) - Ring buffer operations are constant time.
     * Space Complexity: O(1) - No additional space is used.
     *
     * @param data The market data to process.
     */
    public void onMessage(MarketData data) {
        long sequence = ringBuffer.next();
        try {
            ringBuffer.get(sequence).set(data);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public void shutdown() {
        handler.shutdown();
        disruptor.shutdown();
    }
}

