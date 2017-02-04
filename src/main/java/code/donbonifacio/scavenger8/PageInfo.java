package code.donbonifacio.scavenger8;

/**
 * This class represents the data to process by the scavenger. It has
 * information such as the url and the body. This class is used by the
 * workers for communication and logic.
 *
 * This class is immutable.
 */
public final class PageInfo {

    private final String url;

    /**
     * Creates a new PageInfo with the source URL.
     *
     * @param url the url of the page
     */
    private PageInfo(final String url) {
        this.url = url;
    }

    /**
     * Creates a new PageInfo from a given URL.
     *
     * @param url the url
     * @return the PageInfo
     */
    public static PageInfo fromUrl(final String url) {
        return new PageInfo(url);
    }

    /**
     * Gets the associated URL.
     * @return
     */
    public String getUrl() {
        return url;
    }
}
