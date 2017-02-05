package code.donbonifacio.scavenger8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class loads a file with lines containing URLs as a stream.
 * It will then create an associated PageInfo and pass it to the provided
 * queue.
 *
 * The start method will use a runner task in a SingleThreadExecutor, and
 * return right away. If the runner tries to put an element on a full queue,
 * it will block for space.
 *
 * This class is ThreadSafe.
 */
public final class UrlFileLoader {

    private final BlockingQueue<PageInfo> receiver;
    private final String fileName;
    private final ExecutorService executorService;
    private final AtomicLong submittedUrls = new AtomicLong();
    private static final Logger logger = LoggerFactory.getLogger(UrlFileLoader.class);

    /**
     * Creates a new UrlFileLoader.
     *
     * @param fileName the file name to load from
     * @param receiver the queue to put PageInfo's
     */
    public UrlFileLoader(final String fileName, final BlockingQueue<PageInfo> receiver) {
        this.receiver = checkNotNull(receiver);
        this.fileName = checkNotNull(fileName);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Utility class for the main runner. It will gather lines as a stream
     * from the file, and put the associated PageInfo on the queue.
     */
    private class Runner implements Runnable {

        /**
         * Streams lines from a file to a queue.
         */
        @Override
        @SuppressWarnings("squid:S1612") // don't know how to remove this!
        public void run() {
            logger.info("Starting loading lines from {}", fileName);

            try (BufferedReader br = Files.newBufferedReader(Paths.get(fileName))) {

                br.lines()
                        .map(String::trim)
                        .map(String::toLowerCase)
                        .map(domain -> String.format("http://%s", domain))
                        .map(PageInfo::fromUrl)
                        .forEach(info -> submit(info));

                logger.debug("Registering url {}", PageInfo.POISON);

                receiver.put(PageInfo.POISON);

            } catch (IOException e) {
                logger.error("Error reading from the source file", e);
            } catch(InterruptedException e) {
                logger.error("Sending to queue interrupted", e);
                Thread.currentThread().interrupt();
            }
        }

        /**
         * Submits the given page info to the destination queue.
         *
         * @param info the url to submit
         */
        private void submit(final PageInfo info) {
            try {
                logger.debug("Registering url {}", info.getUrl());
                submittedUrls.incrementAndGet();
                receiver.put(info);
            } catch(InterruptedException e) {
                logger.error("Sending to queue interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Gets how may urls have been submitted.
     *
     * @return the submitted url count
     */
    public long getSubmitedUrlsCount() {
        return submittedUrls.get();
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
