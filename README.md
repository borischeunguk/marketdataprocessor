# Market Data Processor

This project implements and benchmarks different market data processing strategies with global and per-symbol throttling, targeting ultra-low latency trading systems.

## Overview

Each `MarketDataProcessor` implementation:

- Accepts real-time market data messages.
- Coalesces updates per symbol.
- Applies:
    - **Global rate limit**: Max N publishes per second.
    - **Per-symbol limit**: No more than 1 update per second per symbol.
- Periodically publishes the latest coalesced data for eligible symbols.

## Implementations

### 1. `DequeMarketDataProcessor`

- Uses standard Java data structures:
    - `ConcurrentHashMap` for symbol tracking.
    - `ConcurrentLinkedDeque` as a sliding window for global rate control.
- Periodically checks eligibility and publishes using a scheduled task.
- Pros: Simple and lock-free.
- Cons: Deque can grow unbounded under high load if not trimmed.

### 2. `BlockingQueueMarketDataProcessor`

- Similar to `DequeMarketDataProcessor`, but:
    - Replaces the Deque with a **bounded** `ArrayBlockingQueue`.
- Pros: More memory-efficient due to bounded capacity.
- Cons: Slightly more blocking under high throughput.

### 3. `LmaxDisruptorMarketDataProcessor`

- Uses [LMAX Disruptor](https://github.com/LMAX-Exchange/disruptor):
    - High-performance `RingBuffer` for event publishing.
    - Dedicated `EventHandler` for consuming and throttling.
- Optimized for **single producer / single consumer** low-latency pipeline.
- Pros: Extremely fast and GC-friendly.
- Cons: More complex to configure; external dependency.

## Benchmarks

Benchmarking is done via [JMH](https://openjdk.org/projects/code-tools/jmh/). It measures:

- **Throughput**: Events processed per second.
- **Latency**: Time taken to accept and enqueue/process a message.
- **Memory usage**: Compared across processor types (WIP).

See `benchmark/` directory for full JMH test suites.

## Assumptions
- onMessage() is always called from a single thread, it will NOT receive more than MAX_GLOBAL_RATE unique symbols per second.
- publishAggregatedMarketData() or related functions like processAndPublish, tryPublish may run in parallel (i.e. multiple consumer threads).
- If there's only one consumer, some locks or synchronization structures can be optimized away for better performance.

## Known Issues
- The test testNoDuplicateSymbolPublishWithinInterval is flaky due to reliance on Thread.sleep() timing. May fail occasionally depending on system scheduling.
- There’s no current mechanism to clean up or cap internal maps (latestBySymbol, lastPublishedTime) if symbols go stale over time.

## Future Improvements
- Experiment with RingBufferUnsafe in LMAX Disruptor for further latency gains.
- Explore commercial options like CoralSequencer (if license allows).
- Clean up common logic and extract reusable components.
- Improve JMH benchmark coverage: include deeper measurement of publishAggregatedMarketData, processAndPublish, and tryPublish.
Add GC pressure and memory profiling.

## Setup

```bash
./gradlew clean jmh
