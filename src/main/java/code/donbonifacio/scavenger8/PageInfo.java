package code.donbonifacio.scavenger8;

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
        this.url = url;
        this.body = body;
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
     * The String representation of the object.
     *
     * @return a string representation of this String
     */
    @Override
    public String toString() {
        if(this == POISON) {
            return "==*== POISON ==*==";
        }
        return String.format("PageInfo[url=%s body=%s]", url, body == null ? "false" : "true");
    }
}
