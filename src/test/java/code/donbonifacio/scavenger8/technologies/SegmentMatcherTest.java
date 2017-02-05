package code.donbonifacio.scavenger8.technologies;


import code.donbonifacio.scavenger8.PageInfo;
import junit.framework.TestCase;

/**
 * Created by pedrosantos on 05/02/17.
 */
public class SegmentMatcherTest extends TestCase {

    public void testMatch() {
        TechnologyMatcher matcher = SegmentMatcher.INSTANCE;
        assertEquals("Segment.io", matcher.getName());

        PageInfo page = PageInfo.fromUrl("http://example.com").withBody("text cdn.segment.com text");
        assertTrue("Should match regex", matcher.isMatch(page));
    }

}