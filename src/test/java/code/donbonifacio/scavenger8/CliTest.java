package code.donbonifacio.scavenger8;

import junit.framework.TestCase;

/**
 * Tests for the command line interface
 */
public class CliTest extends TestCase {

    /**
     * Smoke test for the help option
     *
     * @throws SaftLoaderException
     */
    public void testHelp() {
        Cli.main(new String[] {"-help"});
    }

}
