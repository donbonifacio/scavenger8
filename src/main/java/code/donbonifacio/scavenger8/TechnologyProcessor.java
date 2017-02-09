package code.donbonifacio.scavenger8;

import code.donbonifacio.scavenger8.processors.PipelineProcessor;
import code.donbonifacio.scavenger8.technologies.GoogleTagManagerMatcher;
import code.donbonifacio.scavenger8.technologies.IntercomMatcher;
import code.donbonifacio.scavenger8.technologies.SegmentMatcher;
import code.donbonifacio.scavenger8.technologies.TechnologyMatcher;
import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;

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
public final class TechnologyProcessor implements PipelineProcessor {

    private static final Logger logger = LoggerFactory.getLogger(TechnologyProcessor.class);
    private final WorkerPoolWithGateKeeper workerPool;

    /**
     * The list ot matchers to process. Add new ones here!
     */
    private static final List<TechnologyMatcher> technologyMatchers =
            ImmutableList.<TechnologyMatcher>builder()
                    .add(SegmentMatcher.INSTANCE)
                    .add(IntercomMatcher.INSTANCE)
                    .add(GoogleTagManagerMatcher.INSTANCE)
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
        this.workerPool = new WorkerPoolWithGateKeeper(
                pages,
                technologies,
                nThreads,
                TechnologyProcessor::createTask
        );
    }

    /**
     * Creates a ProcessTechnologies task.
     *
     * @param page the page to process
     * @param outputQueue the output queue
     * @return a runnable
     */
    private static Runnable createTask(final PageInfo page, final BlockingQueue<PageInfo> outputQueue) {
        return new ProcessTechnologies(page, outputQueue);
    }
    /**
     * Utility class that given a PageInfo, will process it against all
     * the registered TechnologyMatcher's.
     */
    private static class ProcessTechnologies implements Runnable {

        private final PageInfo info;
        private final BlockingQueue<PageInfo> outputQueue;

        /**
         * Creates a new ProcessTechnologies.
         *
         * @param info the PageInfo to process
         */
        public ProcessTechnologies(final PageInfo info, final BlockingQueue<PageInfo> outputQueue) {
            this.info = info;
            this.outputQueue = outputQueue;
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
                outputQueue.put(withMatches);

            } catch (InterruptedException e) {
                logger.warn("ProcessTechnologies interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Gets the worker pool in use by this service.
     *
     * @return the worker pool
     */
    @Override
    public WorkerPoolWithGateKeeper getWorkerPool() {
        return workerPool;
    }
}
