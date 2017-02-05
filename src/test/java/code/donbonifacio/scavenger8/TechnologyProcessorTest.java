package code.donbonifacio.scavenger8;

import code.donbonifacio.scavenger8.technologies.TechnologyMatcher;
import junit.framework.TestCase;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Tests for TechnologyProcessor
 */
public final class TechnologyProcessorTest extends TestCase {

    /**
     * Given a PageInfo that does not test anything, checks that no matches
     * are delivered.
     *
     * @throws InterruptedException
     */
    public void testNoTechnologies() throws InterruptedException {
        final PageInfo page = PageInfo.fromUrl("http://httpstat.us/200").withBody("");
        final BlockingQueue<PageInfo> pagesQueue = new LinkedBlockingQueue<>(1);
        final BlockingQueue<PageInfo> technologiesQueue = new LinkedBlockingQueue<>(1);
        final TechnologyProcessor processor = new TechnologyProcessor(pagesQueue, technologiesQueue);

        processor.start();

        pagesQueue.put(page);
        PageInfo processed = technologiesQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull("We should have a proper PageInfo", processed);
        assertEquals(0, processed.getMatches().size());

        pagesQueue.put(PageInfo.POISON);
        PageInfo poison = technologiesQueue.poll(5, TimeUnit.SECONDS);
        assertEquals(poison, PageInfo.POISON);

        assertTrue(processor.isShutdown());
    }

    /**
     * Given a PageInfo with a body that matches something, checks that
     * the match and found and delivered.
     *
     * @throws InterruptedException
     */
    public void testTechnologyMatch() throws InterruptedException {
        final PageInfo page = PageInfo.fromUrl("http://httpstat.us/200").withBody("cdn.segment.com");
        final BlockingQueue<PageInfo> pagesQueue = new LinkedBlockingQueue<>(1);
        final BlockingQueue<PageInfo> technologiesQueue = new LinkedBlockingQueue<>(1);
        final TechnologyProcessor processor = new TechnologyProcessor(pagesQueue, technologiesQueue);

        processor.start();

        pagesQueue.put(page);
        PageInfo processed = technologiesQueue.poll(5, TimeUnit.SECONDS);
        assertNotNull("We should have a proper PageInfo", processed);
        assertEquals(1, processed.getMatches().size());

        TechnologyMatcher matched = processed.getMatches().get(0);
        assertEquals("Segment.io", matched.getName());


        pagesQueue.put(PageInfo.POISON);
        PageInfo poison = technologiesQueue.poll(5, TimeUnit.SECONDS);
        assertEquals(poison, PageInfo.POISON);

        assertTrue(processor.isShutdown());
    }
}
