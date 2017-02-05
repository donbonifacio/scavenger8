package code.donbonifacio.scavenger8;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
    private final System system;

    /**
     * Creates a new Metrics monitor.
     *
     * @param outputFile the file to write the stats
     * @param system the global system
     */
    public MetricsMonitor(final String outputFile,
                          final System system) {
        this.outputFile = checkNotNull(outputFile);
        this.system = checkNotNull(system);
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
                if(system.getOutputSink().isShutdown() && system.getTechnologyProcessor().isShutdown()) {
                    break;
                }
                try {

                    Thread.sleep(1000);
                    StringBuilder builder = new StringBuilder();

                    builder.append(String.format("Used memory: %s MB%n", usedMemory()));
                    builder.append(String.format("URLs submitted: %s%n", system.getUrlFileLoader().getSubmitedUrlsCount()));
                    builder.append(java.lang.System.lineSeparator());

                    builder.append(String.format("URLs queue %d%n", system.getUrlsQueue().size()));
                    builder.append(java.lang.System.lineSeparator());

                    builder.append(String.format("BodyRequester current tasks %d%n", system.getBodyRequester().getTaskCount()));
                    builder.append(String.format("BodyRequester processed tasks %d%n", system.getBodyRequester().getProcessedCount()));
                    builder.append(java.lang.System.lineSeparator());

                    builder.append(String.format("Pages queue %d%n", system.getPagesQueue().size()));
                    builder.append(java.lang.System.lineSeparator());

                    builder.append(String.format("TechnologyProcessor current tasks %d%n", system.getTechnologyProcessor().getTaskCount()));
                    builder.append(String.format("TechnologyProcessor processed tasks %d%n", system.getTechnologyProcessor().getProcessedCount()));
                    builder.append(java.lang.System.lineSeparator());

                    builder.append(String.format("Technologies queue %d%n", system.getTechnologiesQueue().size()));
                    builder.append(java.lang.System.lineSeparator());

                    long last = lastProcessed.get();
                    long totalProcessed = system.getOutputSink().getProcessedCount();
                    long elapsed = totalProcessed - last;
                    builder.append(String.format("OutputSink processed %d%n", totalProcessed));
                    builder.append(String.format("Speed: %d per second%n", elapsed));
                    long estimate100k = elapsed == 0 ? Integer.MAX_VALUE : 100000 / elapsed / 60;
                    builder.append(String.format("It wil take %s minutes to process 100K at this speed%n", estimate100k));
                    builder.append(java.lang.System.lineSeparator());

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
