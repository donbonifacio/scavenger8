package code.donbonifacio.scavenger8;

import junit.framework.TestCase;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Tests for BodyRequest
 */
public final class BodyRequesterTest extends TestCase {

    /**
     * Tests the BodyRequester logic. Given a queue with a PageInfo inside,
     * checks if the request is made and it's put on the destination queue
     * another PageInfo with the body filled.
     *
     * @throws InterruptedException
     */
    public void testRequestUrlBody() throws InterruptedException {
        final PageInfo page = PageInfo.fromUrl("http://httpstat.us/200");
        final BlockingQueue<PageInfo> urlsQueue = new LinkedBlockingQueue<>(1);
        final BlockingQueue<PageInfo> targetQueue = new LinkedBlockingQueue<>(1);
        final BodyRequester requester = new BodyRequester(urlsQueue, targetQueue);

        requester.start();

        urlsQueue.put(page);
        PageInfo withBody = targetQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull("We should have a proper PageInfo", withBody);
        assertEquals("200 OK", withBody.getBody());

        urlsQueue.put(PageInfo.POISON);
        PageInfo poison = targetQueue.poll(5, TimeUnit.SECONDS);
        assertEquals(poison, PageInfo.POISON);

        assertTrue(requester.isShutdown());
    }

}
