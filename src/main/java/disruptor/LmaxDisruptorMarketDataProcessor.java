package disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.RingBuffer;
import utils.MarketData;

import java.util.concurrent.Executors;

public class LmaxDisruptorMarketDataProcessor {
    private final Disruptor<MarketDataEvent> disruptor;
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

