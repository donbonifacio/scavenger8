package code.donbonifacio.scavenger8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    private final BlockingDeque<PageInfo> receiver;
    private final String fileName;
    private final ExecutorService executorService;
    static final Logger logger = LoggerFactory.getLogger(UrlFileLoader.class);

    /**
     * Creates a new UrlFileLoader.
     *
     * @param fileName the file name to load from
     * @param receiver the queue to put PageInfo's
     */
    public UrlFileLoader(final String fileName, final BlockingDeque<PageInfo> receiver) {
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
        public void run() {
            logger.info("Starting loading lines from {}", fileName);

            try (BufferedReader br = Files.newBufferedReader(Paths.get(fileName))) {

                br.lines()
                        .map(String::trim)
                        .forEach(url -> {
                            logger.trace("Registering url {}", url);
                            receiver.push(PageInfo.fromUrl(url));
                        });

            } catch (IOException e) {
                logger.error("Error reading from the source file", e);
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
}
