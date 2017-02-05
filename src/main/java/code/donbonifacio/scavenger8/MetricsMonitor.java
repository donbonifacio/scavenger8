package code.donbonifacio.scavenger8;

import code.donbonifacio.scavenger8.technologies.OutputSink;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Outputs to a given file the running metrics of the system.
 */
public final class MetricsMonitor {

    private static final Logger logger = LoggerFactory.getLogger(MetricsMonitor.class);
    private final ExecutorService executorService;
    private final AtomicLong lastProcessed = new AtomicLong(0);
    private final String outputFile;
    private final Args args;

    public static class Args {
        public UrlFileLoader loader;
        public BodyRequester bodyRequester;
        public TechnologyProcessor processor;
        public OutputSink outputSink;
        public BlockingQueue<PageInfo> urlsQueue;
        public BlockingQueue<PageInfo> pagesQueue;
        public BlockingQueue<PageInfo> technologiesQueue;
    }

    /**
     * Creates a new Metrics monitor.
     *
     * @param outputFile the file to write the stats
     * @param args objec with the remaining arguments
     */
    public MetricsMonitor(final String outputFile,
                          final Args args) {
        this.outputFile = checkNotNull(outputFile);
        this.args = checkNotNull(args);
        checkNotNull(args.loader);
        checkNotNull(args.bodyRequester);
        checkNotNull(args.processor);
        checkNotNull(args.urlsQueue);
        checkNotNull(args.technologiesQueue);
        checkNotNull(args.outputSink);
        checkNotNull(args.pagesQueue);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    /**
     * Runner class that periodically writes the stats to a file.
     */
    private class Runner implements Runnable {

        /**
         * Outputs metrics to output file.
         */
        @Override
        public void run() {
            logger.info("Started dumping metrics to {}...", outputFile);
            final File file = new File(outputFile);
            while (!Thread.currentThread().isInterrupted()) {
                if(args.outputSink.isShutdown() && args.processor.isShutdown()) {
                    break;
                }
                try {

                    Thread.sleep(1000);
                    StringBuilder builder = new StringBuilder();

                    builder.append(String.format("Used memory: %s MB%n", usedMemory()));
                    builder.append(String.format("URLs submitted: %s%n", args.loader.getSubmitedUrlsCount()));
                    builder.append(System.lineSeparator());

                    builder.append(String.format("URLs queue %d/%d%n", args.urlsQueue.size(), args.urlsQueue.remainingCapacity()));
                    builder.append(System.lineSeparator());

                    builder.append(String.format("BodyRequester current tasks %d%n", args.bodyRequester.getTaskCount()));
                    builder.append(String.format("BodyRequester processed tasks %d%n", args.bodyRequester.getProcessedCount()));
                    builder.append(System.lineSeparator());

                    builder.append(String.format("Pages queue %d/%d%n", args.pagesQueue.size(), args.pagesQueue.remainingCapacity()));
                    builder.append(System.lineSeparator());

                    builder.append(String.format("TechnologyProcessor current tasks %d%n", args.processor.getTaskCount()));
                    builder.append(String.format("TechnologyProcessor processed tasks %d%n", args.processor.getProcessedCount()));
                    builder.append(System.lineSeparator());

                    builder.append(String.format("Technologies queue %d/%d%n", args.technologiesQueue.size(), args.technologiesQueue.remainingCapacity()));
                    builder.append(System.lineSeparator());

                    long last = lastProcessed.get();
                    long totalProcessed = args.outputSink.getProcessedCount();
                    long elapsed = totalProcessed - last;
                    builder.append(String.format("OutputSink processed %d%n", totalProcessed));
                    builder.append(String.format("Speed: %d per second%n", elapsed));
                    long estimate100k = elapsed == 0 ? 0 : 100000 / elapsed / 60;
                    builder.append(String.format("It wil take %s minutes to process 100K at this speed%n", estimate100k));
                    builder.append(System.lineSeparator());

                    lastProcessed.set(totalProcessed);

                    Files.write(builder.toString(), file, Charsets.UTF_8);

                } catch(IOException e) {
                    logger.error("Error on metrics", e);
                } catch (InterruptedException e) {
                    logger.error("Metrics interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
        }

        /**
         * Gets the currently used memory.
         *
         * @return the memory in MB
         */
        private long usedMemory() {
            final long raw = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            return raw / 1024 / 1024;
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
