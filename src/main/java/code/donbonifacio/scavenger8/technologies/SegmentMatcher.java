package code.donbonifacio.scavenger8.technologies;

import java.util.regex.Pattern;

/**
 * Macthes PageInfo's for Segment.io
 */
public class SegmentMatcher extends RegexScrapperMatcher {

    private static final Pattern PATTERN = Pattern.compile(".*cdn\\.segment\\.com.*");
    public static final SegmentMatcher INSTANCE = new SegmentMatcher();

    /**
     * Disallow creating instances
     */
    private SegmentMatcher() {
    }

    /**
     * Gets the name of this matcher.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return "Segment.io";
    }

    /**
     * Gets the regex pattern for this matcher.
     *
     * @return the pattern
     */
    @Override
    protected Pattern getPattern() {
        return PATTERN;
    }

}
