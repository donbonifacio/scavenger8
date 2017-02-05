package code.donbonifacio.scavenger8;

import code.donbonifacio.scavenger8.technologies.OutputSink;

import java.util.concurrent.BlockingQueue;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class gathers all the system components and is able to start
 * and them all.
 */
public final class System {

    private UrlFileLoader loader;
    private BodyRequester bodyRequester;
    private TechnologyProcessor technologyProcessor;
    private OutputSink outputSink;
    private MetricsMonitor metricsMonitor;
    private BlockingQueue<PageInfo> urlsQueue;
    private BlockingQueue<PageInfo> pagesQueue;
    private BlockingQueue<PageInfo> technologiesQueue;

    /**
     * Public ctor
     */
    public System() {
        // nothing to do, object should be built with the
        // builder methods
    }

    /**
     * Starts all system components.
     *
     * @return this system
     */
    public System start() {
        loader.start();
        bodyRequester.start();
        technologyProcessor.start();
        outputSink.start();
        metricsMonitor.start();
        return this;
    }

    /**
     * Sets the file loader.
     *
     * @param fileName the source file name
     * @return the system
     */
    public System createUrlFileLoader(final String fileName) {
        this.loader = new UrlFileLoader(fileName, urlsQueue);
        return this;
    }

    /**
     * Sets the body requester.
     *
     * @return this system
     */
    public System createBodyRequester() {
        this.bodyRequester = new BodyRequester(urlsQueue, pagesQueue, 80);
        return this;
    }

    /**
     * Sets the technology processor.
     *
     * @return this system
     */
    public System createTechnologyProcessor() {
        this.technologyProcessor = new TechnologyProcessor(pagesQueue, technologiesQueue, 4);
        return this;
    }

    /**
     * Sets the output sink.
     *
     * @return this system
     */
    public System createOutputSink() {
        this.outputSink = new OutputSink(technologiesQueue);
        return this;
    }

    /**
     * Creates the metrics monitor.
     *
     * @param outputFileName the output file name to use
     * @return this system
     */
    public System createMetricsMonitor(final String outputFileName) {
        this.metricsMonitor = new MetricsMonitor(
                checkNotNull(outputFileName),
                this
        );
        return this;
    }

    /**
     * Sets the urls queue.
     *
     * @param queue the urls queue
     * @return this system
     */
    public System setUrlsQueue(final BlockingQueue<PageInfo> queue) {
        this.urlsQueue = checkNotNull(queue);
        return this;
    }

    /**
     * Sets the pages queue.
     *
     * @param queue the pages queue
     * @return this system
     */
    public System setPagesQueue(final BlockingQueue<PageInfo> queue) {
        this.pagesQueue = checkNotNull(queue);
        return this;
    }

    /**
     * Sets the technologies queue.
     *
     * @param queue the technologies
     * @return this system
     */
    public System setTechnologiesQueue(final BlockingQueue<PageInfo> queue) {
        this.technologiesQueue = checkNotNull(queue);
        return this;
    }

    /**
     * Gets the output sink.
     *
     * @return the output sink
     */
    public OutputSink getOutputSink() {
        return outputSink;
    }

    /**
     * Gets the technology processor.
     *
     * @return the technology processor
     */
    public TechnologyProcessor getTechnologyProcessor() {
        return technologyProcessor;
    }

    /**
     * Gets the url file loader.
     *
     * @return the file loader
     */
    public UrlFileLoader getUrlFileLoader() {
        return loader;
    }

    /**
     * Gets the urls queue.
     *
     * @return the urls queue
     */
    public BlockingQueue<PageInfo> getUrlsQueue() {
        return urlsQueue;
    }

    /**
     * Gets the body requester.
     *
     * @return the body requester
     */
    public BodyRequester getBodyRequester() {
        return bodyRequester;
    }

    /**
     * Gets the pages queue.
     *
     * @return the pages queue
     */
    public BlockingQueue<PageInfo> getPagesQueue() {
        return pagesQueue;
    }

    /**
     * Gets the technologies queue.
     *
     * @return the tech queue
     */
    public BlockingQueue<PageInfo> getTechnologiesQueue() {
        return technologiesQueue;
    }
}
