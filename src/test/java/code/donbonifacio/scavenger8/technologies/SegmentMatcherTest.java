package code.donbonifacio.scavenger8.technologies;

import code.donbonifacio.scavenger8.PageInfo;
import junit.framework.TestCase;

/**
 * Test SegmentMatcher
 */
public class SegmentMatcherTest extends TestCase {

    private final static TechnologyMatcher SUT = SegmentMatcher.INSTANCE;

    /**
     * Tests the matcher name
     */
    public void testMatcherName() {
        assertEquals("Segment.io", SUT.getName());
    }

    /**
     * Tests a positive match
     */
    public void testSuccessMatch() {
        PageInfo page = PageInfo.fromUrl("http://example.com").withBody("text cdn.segment.com text");
        assertTrue("Should match regex", SUT.isMatch(page));
    }

    /**
     * Tests a failed match
     */
    public void testFailedMatch() {
        PageInfo page = PageInfo.fromUrl("http://example.com").withBody("wasabi");
        assertFalse("Should not match regex", SUT.isMatch(page));
    }

}