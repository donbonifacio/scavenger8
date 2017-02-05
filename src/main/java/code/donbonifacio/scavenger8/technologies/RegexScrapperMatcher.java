package code.donbonifacio.scavenger8.technologies;

import code.donbonifacio.scavenger8.PageInfo;

import java.util.regex.Pattern;

/**
 * Base utility class that given a PageInfo will apply the given regex
 * pattern to find a match.
 */
public abstract class RegexScrapperMatcher implements TechnologyMatcher {

    /**
     * Gets the regex pattern to use.
     *
     * @return the pattern
     */
    protected abstract Pattern getPattern();

    /**
     * Verifies that a PageInfo matches this matcher.
     *
     * @param info the PageInfo
     * @return true if it matches
     */
    @Override
    public boolean isMatch(PageInfo info) {
        return getPattern().matcher(info.getBody()).matches();
    }
}
