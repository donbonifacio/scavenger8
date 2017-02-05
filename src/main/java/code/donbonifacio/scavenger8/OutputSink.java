package code.donbonifacio.scavenger8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class grabs a stream of PageInfo and just outputs it to the logger.
 */
public final class OutputSink {

    private final BlockingQueue<PageInfo> input;
    private final ExecutorService executorService;
    private static final Logger logger = LoggerFactory.getLogger(OutputSink.class);
    private final AtomicLong processedCounter = new AtomicLong();

    /**
     * Creates a new OutputSink.
     *
     * @param input the input queue
     */
    public OutputSink(final BlockingQueue<PageInfo> input) {
        this.input = checkNotNull(input);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Utility class for the main runner.
     */
    private class Runner implements Runnable {

        /**
         * Streams PageInfos to the logger
         */
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    PageInfo page = input.take();

                    logger.info("Result: {}", page);

                    if (PageInfo.isPoison(page)) {
                        break;
                    }

                    processedCounter.incrementAndGet();

                } catch (InterruptedException e) {
                    logger.error("OutputSink interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
        }

    }

    /**
     * Starts the main runner, in another Thread, and returns right away.
     */
    public void start() {
        executorService.execute(new Runner());
        executorService.shutdown();
    }

    /**
     * True if this service is shutdown.
     *
     * @return true if it's shutdown
     */
    public boolean isShutdown() {
        return executorService.isShutdown();
    }

    /**
     * Gets the number of processed objects.
     *
     * @return the total count
     */
    public long getProcessedCount() {
        return processedCounter.get();
    }
}
