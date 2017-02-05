package code.donbonifacio.scavenger8.technologies;

import java.util.regex.Pattern;

/**
 * Macthes PageInfo's for Segment.io
 */
public final class IntercomMatcher extends RegexScrapperMatcher {

    private static final Pattern PATTERN = Pattern.compile(".*widget\\.intercom\\.io\\/widget.*", Pattern.DOTALL);
    public static final TechnologyMatcher INSTANCE = new IntercomMatcher();

    /**
     * Disallow creating instances
     */
    private IntercomMatcher() {
    }

    /**
     * Gets the name of this matcher.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return "Intercom.io";
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
