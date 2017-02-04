package code.donbonifacio.scavenger8;

import junit.framework.TestCase;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Tests for UrlFileLoader
 */
public final class UrlFileLoaderTest extends TestCase {

    /**
     * Reads the SingleLine.txt file that has one URL and checks that
     * the associated PageInfo is created and put on a queue.
     *
     * @throws InterruptedException
     */
    public void testSingleLineFile() throws InterruptedException {
        final String fileName = "src/test/resources/SingleLine.txt";
        final BlockingQueue<PageInfo> queue = new LinkedBlockingQueue<>(1);
        UrlFileLoader loader = new UrlFileLoader(fileName, queue);
        loader.start();
        PageInfo page = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull("Expected an object on the queue", page);
        assertEquals(page.getUrl(), "http://www.google.com");

        PageInfo poison = queue.poll(5, TimeUnit.SECONDS);
        assertEquals("A poison should be sent last", poison, PageInfo.POISON);

        assertTrue("After finishing should be shutdown", loader.isShutdown());
    }

}
