package RegExServer;

// FILE: RegExPage.java

import java.io.IOException;

/**
 * Each page in the RegEx system will extend this class so that the handler can
 * get the pages content.
 *
 * @author Kevin J. Becker
 * @version 03/24/2019
 */
public abstract class RegExPage {
    /**
     * Gets the byte form of the page's full content.
     *
     * @return  The byte-level form of the page's content.
     * @throws IOException  If any IOException is encountered, it is thrown to the caller.
     */
    public abstract byte[] getPageContent() throws IOException;
}
