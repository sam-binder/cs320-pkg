package RegExServer;

// FILE: RegExCreateAccount.java

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * A RegExPage class used to generate the account creation page.
 */
public class RegExCreateAccount extends RegExPage {
    /**
     *  The error message to generate the page with.
     */
    private String error;

    /**
     * If there is no error to be displayed on the home page, this constant can be used
     * to identify that.
     */
    public static final String NO_ERROR = null;

    /**
     * URI of this page's base HTML file.
     */
    private static String pageURI = RegExHttpHandler.DOCUMENT_ROOT + "/create-account/index.html";

    /**
     * Creates a new RegExCreateAccount instance with error message error.
     *
     * @param error  The error message to generate the page with.
     */
    public RegExCreateAccount(String error) {
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
            RegExDialog.getErrorDialogHTML(this.error)
        );

        // return our page content as bytes
        return pageContent.getBytes();
    }
}
