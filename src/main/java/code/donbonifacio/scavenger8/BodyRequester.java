package code.donbonifacio.scavenger8;

import code.donbonifacio.scavenger8.processors.PipelineProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.BlockingQueue;

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
public final class BodyRequester implements PipelineProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BodyRequester.class);
    private final WorkerPoolWithGateKeeper workerPool;

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
        this.workerPool = new WorkerPoolWithGateKeeper(
                urlsQueue,
                processorsQueue,
                nThreads,
                BodyRequester::createTask
        );
    }

    /**
     * Creates a RequestBody task.
     *
     * @param page the page to process
     * @param outputQueue the output queue
     * @return a runnable
     */
    private static Runnable createTask(final PageInfo page, final BlockingQueue<PageInfo> outputQueue) {
        return new RequestBody(page, outputQueue);
    }

    /**
     * Utility class that given a PageInfo with an url, performs an HTTP
     * request and obtains the response body. It will then put on the
     * destination queue a new PageInfo with the body.
     */
    private static class RequestBody implements Runnable {

        private final PageInfo info;
        private final BlockingQueue<PageInfo> outputQueue;

        /**
         * Create a RequestBody
         * @param info the page to process
         */
        RequestBody(final PageInfo info, final BlockingQueue<PageInfo> outputQueue) {
            this.info = info;
            this.outputQueue = outputQueue;
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
            con.setRequestProperty("User-Agent", "scavenger8");

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
                Thread.currentThread().setName(url);
                HttpURLConnection con = getHttpConnection(url);

                final int responseCode = con.getResponseCode();
                logger.trace("Got status {} for {} Location:{}", responseCode, info, con.getHeaderField("Location"));

                if(responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                        responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                    final String newUrl = con.getHeaderField("Location");
                    logger.debug("Redirecting {} to {}", url, newUrl);
                    Thread.currentThread().setName(newUrl);
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

                outputQueue.put(withBody);

            } catch(IOException ex) {
                logger.debug("Error on HTTP request", ex);
            } catch (InterruptedException e) {
                logger.warn("BodyRequester get response interrupted", e);
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
