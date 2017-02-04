package code.donbonifacio.scavenger8;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Command line interface to access the lib's functionality.
 */
public final class Cli {

    static final Logger logger = LoggerFactory.getLogger(Cli.class);

    /**
     * Represents CLI arguments passed to the program
     *
     */
    private static class CliArgs {

        /**
         * Set the file
         */
        @Parameter(names = "-file", description = "Source file")
        public String fileName;

        /**
         * Show help/usage flag
         */
        @Parameter(names = "-help", description = "Shows usage")
        public boolean help = false;

        /**
         * Represents this object as a String
         *
         * @return a representation of the args
         */
        @Override public String toString() {
            if(help) {
                return "Args: help";
            }
            return "??";
        }
    }

    /**
     * Disable creating objects of this type
     */
    private Cli() {
        throw new AssertionError();
    }

    /**
     * The main entry point of the CLI
     *
     * @param argv the input arguments
     */
    public static void main( String[] argv ) {

        CliArgs args = new CliArgs();
        JCommander commander = new JCommander(args, argv);
        logger.trace("Input {}", args);

        if(args.help) {
            commander.usage();
        } else {
            logger.info("scavenger8");

            BlockingQueue<PageInfo> urls = new LinkedBlockingQueue<>(100);
            BlockingQueue<PageInfo> pages = new LinkedBlockingQueue<>(100);

            UrlFileLoader loader = new UrlFileLoader(args.fileName, urls);
            BodyRequester requester = new BodyRequester(urls, pages);

            loader.start();
            requester.start();
        }

    }

}
