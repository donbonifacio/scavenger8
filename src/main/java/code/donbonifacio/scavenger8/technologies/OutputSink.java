package code.donbonifacio.scavenger8.technologies;

import code.donbonifacio.scavenger8.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class grabs a stream of PageInfo and just outputs it to the logger.
 */
public final class OutputSink {

    private final BlockingQueue<PageInfo> input;
    private final ExecutorService executorService;
    static final Logger logger = LoggerFactory.getLogger(OutputSink.class);

    /**
     * Creates a new OutputSink.
     *
     * @param fileName the file name to load from
     * @param receiver the queue to put PageInfo's
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
}
