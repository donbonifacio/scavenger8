package code.donbonifacio.scavenger8;

import code.donbonifacio.scavenger8.technologies.SegmentMatcher;
import code.donbonifacio.scavenger8.technologies.TechnologyMatcher;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The TechnologyProcessor is the service of the pipeline that given
 * a stream of PageInfo's, will try to match technologies to them. It has
 * a collection of technology matchers that will be processed.
 *
 * If it receives the special PageInfo.POISON mark, it will stop gathering
 * work, but will wait for the current work to be completed.
 *
 * This class is ThreadSafe.
 */
public final class TechnologyProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TechnologyProcessor.class);
    private final BlockingQueue<PageInfo> pages;
    private final BlockingQueue<PageInfo> technologies;
    private final ExecutorService executorService;
    private final ExecutorService gateKeeper;
    private final AtomicLong taskCounter = new AtomicLong();
    private final AtomicLong processedCounter = new AtomicLong();

    /**
     * The list ot matchers to process. Add new ones here!
     */
    private static final List<TechnologyMatcher> technologyMatchers =
            ImmutableList.<TechnologyMatcher>builder()
                    .add(SegmentMatcher.INSTANCE)
                    .build();

    /**
     * Creates a new TechnologyProcessor.
     *
     * @param pages the input queue
     * @param technologies the output queue
     */
    public TechnologyProcessor(final BlockingQueue<PageInfo> pages, final BlockingQueue<PageInfo> technologies) {
        this(pages, technologies, 4);
    }

    /**
     * Creates a new TechnologyProcessor.
     *
     * @param pages the input queue
     * @param technologies the output queue
     * @param nThreads number of worker threads to spawn
     */
    public TechnologyProcessor(final BlockingQueue<PageInfo> pages, final BlockingQueue<PageInfo> technologies, int nThreads) {
        this.pages = checkNotNull(pages);
        this.technologies = checkNotNull(technologies);
        this.gateKeeper = Executors.newSingleThreadExecutor();
        this.executorService = Executors.newFixedThreadPool(nThreads);
    }

    /**
     * Utility class that will gather work, and send it to the worker pool.
     */
    private class Runner implements Runnable {

        /**
         * Main loop, gathers and distributes work.
         */
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try{
                    PageInfo page = pages.take();

                    if(PageInfo.isPoison(page)) {
                        logger.trace("POISON Received!");

                        logger.trace("Shutting down worker pool...");
                        executorService.shutdown();

                        logger.trace("Waiting current tasks to finish...");
                        executorService.awaitTermination(10, TimeUnit.MINUTES);

                        logger.debug("Work finished, submitting {}", page);
                        technologies.put(page);
                        break;
                    }

                    logger.trace("Processing technology matchers {}", page);
                    taskCounter.incrementAndGet();
                    executorService.execute(new ProcessTechnologies(page));

                } catch (InterruptedException e) {
                    logger.warn("Main TechnologyProcessor interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }

        }

    }

    /**
     * Utility class that given a PageInfo, will process it against all
     * the registered TechnologyMatcher's.
     */
    private class ProcessTechnologies implements Runnable {

        private final PageInfo info;

        /**
         * Creates a new ProcessTechnologies.
         *
         * @param info the PageInfo to process
         */
        public ProcessTechnologies(final PageInfo info) {
            this.info = info;
        }

        /**
         * Finds technology matches for the given PageInfo.
         */
        @Override
        public void run() {
            try {

                List<TechnologyMatcher> matches = technologyMatchers
                        .stream()
                        .filter(matcher -> matcher.isMatch(info))
                        .collect(ImmutableList.toImmutableList());

                PageInfo withMatches = info.withMatches(matches);

                logger.debug("Processed {}", withMatches);
                taskCounter.decrementAndGet();
                processedCounter.incrementAndGet();
                technologies.put(withMatches);

            } catch (InterruptedException e) {
                logger.warn("ProcessTechnologies interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Starts the main runner, in another Thread, and returns right away.
     */
    public void start() {
        gateKeeper.execute(new Runner());
        gateKeeper.shutdown();
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
     * Gets the number of current tasks running and/or scheduled to run.
     *
     * @return the count of tasks
     */
    public long getTaskCount() {
        return taskCounter.get();
    }

    /**
     * Gets the number of processed tasks.
     *
     * @return the count of processed tasks
     */
    public long getProcessedCount() {
        return processedCounter.get();
    }
}
