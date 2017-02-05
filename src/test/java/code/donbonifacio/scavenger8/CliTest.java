package code.donbonifacio.scavenger8;

import junit.framework.TestCase;

/**
 * Tests for the command line interface
 */
public class CliTest extends TestCase {

    /**
     * Smoke test for the help option
     *
     */
    public void testHelp() {
        try {
            Cli.main(new String[] {"-help"});
        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

    /**
     * Smoke test for a basic file
     */
    public void testSingleLineFile() {
        try {
            final String fileName = "src/test/resources/SingleLine.txt";
            Cli.main(new String[]{"-file", fileName});
        } catch(Exception ex) {
            fail(ex.getMessage());
        }
    }

}
