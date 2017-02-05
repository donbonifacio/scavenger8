package code.donbonifacio.scavenger8.technologies;

import code.donbonifacio.scavenger8.PageInfo;

/**
 * Represents an object capable of matching a technology to a PageInfo.
 */
public interface TechnologyMatcher {

    /**
     * Gets a descriptive name for the matcher.
     *
     * @return the name
     */
    String getName();

    /**
     * Performs the match logic.
     *
     * @param info the PageInfo to match
     * @return true if matches
     */
    boolean isMatch(PageInfo info);
}
