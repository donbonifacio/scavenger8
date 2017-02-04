package code.donbonifacio.scavenger8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    static final Logger logger = LoggerFactory.getLogger(BodyRequester.class);
    private final BlockingDeque<PageInfo> urlsQueue;
    private final BlockingDeque<PageInfo> processorsQueue;
    private final ExecutorService executorService;
    private final ExecutorService gateKeeper;
    private final int nThreads;

    /**
     * Creates a new BodyRequester.
     *
     * @param urlsQueue the source queue
     * @param processorsQueue the destination queue
     */
    public BodyRequester(final BlockingDeque<PageInfo> urlsQueue, final BlockingDeque<PageInfo> processorsQueue) {
        this(urlsQueue, processorsQueue, 12);
    }

    /**
     * Creates a new BodyRequester.
     *
     * @param urlsQueue the source queue
     * @param processorsQueue the destination queue
     * @param nThreads the number of worker threads to spawn
     */
    public BodyRequester(final BlockingDeque<PageInfo> urlsQueue, final BlockingDeque<PageInfo> processorsQueue, int nThreads) {
        this.urlsQueue = urlsQueue;
        this.processorsQueue = processorsQueue;
        this.nThreads = nThreads;
        this.executorService = Executors.newFixedThreadPool(nThreads);
        this.gateKeeper = Executors.newSingleThreadExecutor();
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

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try{
                    PageInfo page = urlsQueue.take();

                    if(page.equals(PageInfo.POISON)) {
                        logger.trace("Work finished, submitting {}", page);
                        processorsQueue.put(page);
                        break;
                    }

                    logger.trace("Processing body request for {}", page);
                    executorService.execute(new RequestBody(page));

                } catch (InterruptedException e) {
                    logger.warn("Main BodyRequester interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }

            logger.trace("Shutting down worker pool...");
            executorService.shutdown();
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
         * Performs the necessary HTTP requests to obtain the response body
         * for an URL.
         */
        public void run() {
            final String url = info.getUrl();
            logger.trace("Requesting page body for {}", info);

            try {

                final URL obj = new URL(url);
                final HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                con.setRequestMethod("GET");
                con.setRequestProperty("User-Agent", "https://github.com/donbonifacio/scavenger8");

                final int responseCode = con.getResponseCode();
                logger.debug("Got status {} for {}", responseCode, info);

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuffer response = new StringBuffer();

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                in.close();

                PageInfo withBody = info.withBody(response.toString());
                logger.trace("Submitting {}", withBody);

                processorsQueue.put(withBody);

            } catch(IOException | InterruptedException ex) {
                logger.error("Error on HTTP request", ex);
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

}