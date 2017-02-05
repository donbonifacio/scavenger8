package code.donbonifacio.scavenger8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The BodyRequester class is responsible for fetching raw PageInfo's from
 * a source queue, execute the necessary HTTP requests to obtain the response
 * body for them, and put on the destination queue a new PageInfo with the
 * associated body.
 *
 * When the special PageInfo.POISON object is received, the service will shutdown
 * the worker pool. No more work will be fetch from the source queue, but
 * the current executing tasks will still finish.
 */
public final class BodyRequester {

    private static final Logger logger = LoggerFactory.getLogger(BodyRequester.class);
    private final BlockingQueue<PageInfo> urlsQueue;
    private final BlockingQueue<PageInfo> processorsQueue;
    private final ExecutorService executorService;
    private final ExecutorService gateKeeper;
    private final AtomicLong taskCounter = new AtomicLong();
    private final AtomicLong processedCounter = new AtomicLong();
    private final int nThreads;

    /**
     * Creates a new BodyRequester.
     *
     * @param urlsQueue the source queue
     * @param processorsQueue the destination queue
     */
    public BodyRequester(final BlockingQueue<PageInfo> urlsQueue, final BlockingQueue<PageInfo> processorsQueue) {
        this(urlsQueue, processorsQueue, 12);
    }

    /**
     * Creates a new BodyRequester.
     *
     * @param urlsQueue the source queue
     * @param processorsQueue the destination queue
     * @param nThreads the number of worker threads to spawn
     */
    public BodyRequester(final BlockingQueue<PageInfo> urlsQueue, final BlockingQueue<PageInfo> processorsQueue, int nThreads) {
        this.urlsQueue = urlsQueue;
        this.processorsQueue = processorsQueue;
        this.executorService = Executors.newFixedThreadPool(nThreads);
        this.gateKeeper = Executors.newSingleThreadExecutor();
        this.nThreads = nThreads;
    }

    /**
     * Utility class that will control the flow on this service. It will
     * receive raw PageInfo's with just an URL, and will delegate the response
     * fetching to the `executorService`.
     *
     * However, if a special mark PageInfo.POISON is received, the `executorService`
     * will be shutdown and the work is considered completed.
     */
    private class Runner implements Runnable {

        /**
         * Main loop, gathers and distributes work.
         */
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try{
                    if(taskCounter.get() >= nThreads) {
                        // back pressure
                        Thread.sleep(1000);
                        continue;
                    }

                    PageInfo page = urlsQueue.take();

                    if(PageInfo.isPoison(page)) {
                        logger.trace("POISON Received!");

                        logger.trace("Shutting down worker pool...");
                        executorService.shutdown();

                        logger.trace("Waiting current tasks to finish...");
                        executorService.awaitTermination(10, TimeUnit.MINUTES);

                        logger.debug("Work finished, submitting {}", page);
                        processorsQueue.put(page);

                        Thread.currentThread().interrupt();
                    } else {
                        logger.trace("Processing body request for {}", page);
                        taskCounter.incrementAndGet();
                        executorService.execute(new RequestBody(page));
                    }

                } catch (InterruptedException e) {
                    logger.warn("Main BodyRequester interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }

        }

    }

    /**
     * Utility class that given a PageInfo with an url, performs an HTTP
     * request and obtains the response body. It will then put on the
     * destination queue a new PageInfo with the body.
     */
    private class RequestBody implements Runnable {

        private final PageInfo info;

        /**
         * Create a RequestBody
         * @param info the page to process
         */
        RequestBody(final PageInfo info) {
            this.info = info;
        }

        /**
         * Creates a configured HttpConnection.
         *
         * @param url the url for the connection
         * @return the HttpConnection
         * @throws IOException
         */
        private HttpURLConnection getHttpConnection(final String url)
            throws IOException {

            final URL obj = new URL(url);
            final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setReadTimeout(5000);
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "https://github.com/donbonifacio/scavenger8");

            return con;
        }

        /**
         * Performs the necessary HTTP requests to obtain the response body
         * for an URL.
         */
        @Override
        public void run() {
            final String url = info.getUrl();
            logger.trace("Requesting page body for {}", info);

            try {
                HttpURLConnection con = getHttpConnection(url);

                final int responseCode = con.getResponseCode();
                logger.trace("Got status {} for {} Location:{}", responseCode, info, con.getHeaderField("Location"));

                if(responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    final String newUrl = con.getHeaderField("Location");
                    logger.debug("Redirecting {} to {}", url, newUrl);
                    con = getHttpConnection(newUrl);
                }

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder response = new StringBuilder();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                PageInfo withBody = info.withBody(response.toString());
                logger.debug("Submitting {}", withBody);

                taskCounter.decrementAndGet();
                processedCounter.incrementAndGet();
                processorsQueue.put(withBody);

            } catch(IOException ex) {
                logger.debug("Error on HTTP request", ex);
            } catch (InterruptedException e) {
                logger.warn("BodyRequester get response interrupted", e);
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
