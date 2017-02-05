package code.donbonifacio.scavenger8.technologies;

import code.donbonifacio.scavenger8.PageInfo;
import junit.framework.TestCase;

/**
 * Test GoogleTagManagerMatcher
 */
public class GoogleTagManagerMatcherTest extends TestCase {

    private final static TechnologyMatcher SUT = GoogleTagManagerMatcher.INSTANCE;

    /**
     * Tests the matcher name
     */
    public void testMatcherName() {
        assertEquals("Google Tag Manager", SUT.getName());
    }

    /**
     * Tests a positive match
     */
    public void testSuccessMatch() {
        PageInfo page = PageInfo.fromUrl("http://example.com").withBody("text\n //www.googletagmanager.com/ns.html\n text");
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
