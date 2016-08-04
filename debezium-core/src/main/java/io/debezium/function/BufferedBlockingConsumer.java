/*
 * Copyright Debezium Authors.
 * 
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.function;

import java.util.function.Function;

/**
 * A {@link BlockingConsumer} that retains a maximum number of values in a buffer before sending them to
 * a delegate consumer. Note that any buffered values may need to be {@link #flush() flushed} periodically.
 * <p>
 * This maintains the same order of the values.
 * 
 * @param <T> the type of the input to the operation
 * @author Randall Hauch
 */
public interface BufferedBlockingConsumer<T> extends BlockingConsumer<T> {

    /**
     * Flush all of the buffered values to the delegate.
     * 
     * @throws InterruptedException if the thread is interrupted while this consumer is blocked
     */
    public default void flush() throws InterruptedException {
        flush(t -> t);
    }

    /**
     * Flush all of the buffered values to the delegate by first running each buffered value through the given function
     * to generate a new value to be flushed to the delegate consumer.
     * 
     * @param function the function to apply to the values that are flushed
     * @throws InterruptedException if the thread is interrupted while this consumer is blocked
     */
    public void flush(Function<T, T> function) throws InterruptedException;

    /**
     * Get a {@link BufferedBlockingConsumer} that buffers just the last value seen by the consumer.
     * When another value is then added to the consumer, this buffered consumer will push the prior value into the delegate
     * and buffer the latest.
     * <p>
     * The resulting consumer is not threadsafe.
     * 
     * @param delegate the delegate to which values should be flushed; may not be null
     * @return the blocking consumer that buffers a single value at a time; never null
     */
    public static <T> BufferedBlockingConsumer<T> bufferLast(BlockingConsumer<T> delegate) {
        return new BufferedBlockingConsumer<T>() {
            private T last;

            @Override
            public void accept(T t) throws InterruptedException {
                if (last != null) delegate.accept(last);
                last = t;
            }

            @Override
            public void flush(Function<T, T> function) throws InterruptedException {
                if (last != null) {
                    try {
                        delegate.accept(function.apply(last));
                    } finally {
                        last = null;
                    }
                }
            }
        };
    }
}
