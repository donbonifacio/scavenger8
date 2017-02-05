package code.donbonifacio.scavenger8.technologies;


import java.util.regex.Pattern;

/**
 * Macthes PageInfo's for Google Tag Manager
 */
public final class GoogleTagManagerMatcher extends RegexScrapperMatcher {

    private static final Pattern PATTERN = Pattern.compile(".*\\/\\/www\\.googletagmanager\\.com\\/ns\\.html.*", Pattern.DOTALL);
    public static final TechnologyMatcher INSTANCE = new GoogleTagManagerMatcher();

    /**
     * Disallow creating instances
     */
    private GoogleTagManagerMatcher() {
    }

    /**
     * Gets the name of this matcher.
     *
     * @return the name
     */
    @Override
    public String getName() {
        return "Google Tag Manager";
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
