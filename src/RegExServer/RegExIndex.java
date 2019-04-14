package RegExServer;

// FILE: RegExIndex.java

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * RegExPage class used to format the Index page.
 *
 * @author Kevin J. Becker (kjb2503)
 * @version 03/20/2019
 */
public class RegExIndex extends RegExPage {
    /**
     * URI of this page's base HTML file.
     */
    private static String pageURI = RegExHttpHandler.DOCUMENT_ROOT + "/index.html";

    /**
     * If there is no error to be displayed on the home page, this constant can be used
     * to identify that.
     */
    public final static String NO_ERROR = null;

    /**
     * The error message to print on the page.
     */
    private String error;

    /**
     * Creates a new RegExIndex object with error message of error.
     *
     * @param error  The error message to display at the top of the page.
     */
    public RegExIndex(String error) {
        this.error = error;
    }

    /**
     * Returns the byte-level form of the page's content.
     *
     * @return The byte-level form of the page's content.
     * @throws IOException If any IOException is encountered, it is thrown out to the caller.
     */
    @Override
    public byte[] getPageContent() throws IOException {
        // attempts to return our login screen if an error is
        // encountered an IOException is thrown
        String pageContent = new String(
            Files.readAllBytes(
                Paths.get(pageURI)
            ),
            StandardCharsets.UTF_8
        );

        // replaces error dialogue with
        pageContent = pageContent.replace(
            "@{error-dialog}",
            RegExErrorDialog.getErrorDialogHTML(this.error)
        );

        // return our page content as bytes
        return pageContent.getBytes();
    }
}
