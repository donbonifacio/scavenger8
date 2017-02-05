package code.donbonifacio.scavenger8;

import code.donbonifacio.scavenger8.technologies.TechnologyMatcher;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This class represents the data to process by the scavenger. It has
 * information such as the url and the body. This class is used by the
 * workers for communication and logic.
 *
 * This class is immutable.
 */
public final class PageInfo {

    private final String url;
    private final String body;
    private final List<TechnologyMatcher> matches;
    public static final PageInfo POISON = new PageInfo(null);

    /**
     * Creates a new PageInfo with the source URL.
     *
     * @param url the url of the page
     */
    private PageInfo(final String url) {
        this(url, null);
    }

    /**
     * Creates a new PageInfo with the URL and the body.
     *
     * @param url the url
     * @param body the body
     */
    private PageInfo(final String url, final String body) {
        this(url, body, ImmutableList.of());
    }

    /**
     * Creates a new PageInfo with URL, body and matched technologies.
     *
     * @param url the url
     * @param body the body
     * @param matches the technology matches
     */
    private PageInfo(final String url, final String body, final List<TechnologyMatcher> matches) {
        this.url = url;
        this.body = body;
        this.matches = matches;
    }

    /**
     * Returns true if the given PageInfo is a poison marker.
     *
     * @param page the PageInfo to test
     * @return true if poison
     */
    public static boolean isPoison(final PageInfo page) {
        return page == POISON;
    }

    /**
     * Creates a new PageInfo from a given URL.
     *
     * @param url the url
     * @return the PageInfo
     */
    public static PageInfo fromUrl(final String url) {
        return new PageInfo(checkNotNull(url));
    }

    /**
     * Returns a copy of this PageInfo, with the given body.
     *
     * @param body the associated body
     * @return a new PageInfo
     */
    public PageInfo withBody(final String body) {
        return new PageInfo(url, body);
    }

    /**
     * Returns a copy of this PageInfo, with the given matches.
     *
     * @param matches the technology mathes
     * @return a new PageInfo
     */
    public PageInfo withMatches(final List<TechnologyMatcher> matches) {
        return new PageInfo(url, body, ImmutableList.copyOf(matches));
    }

    /**
     * Gets the associated URL.
     *
     * @return the string url
     */
    public String getUrl() {
        return url;
    }

    /**
     * Gets the body for this page.
     *
     * @return the body string
     */
    public String getBody() {
        return body;
    }

    /**
     * Gets the technology matches.
     *
     * @return the technology mathes
     */
    public List<TechnologyMatcher> getMatches() {
        return matches;
    }

    /**
     * The String representation of the object.
     *
     * @return a string representation of this String
     */
    @Override
    public String toString() {
        if(isPoison(this)) {
            return "==*== POISON ==*==";
        }
        return String.format("PageInfo[url=%s body=%s matches=%s]",
                url,
                body == null ? "false" : "true",
                matches.stream()
                        .map(TechnologyMatcher::getName)
                        .collect(Collectors.joining(","))
        );
    }
}
