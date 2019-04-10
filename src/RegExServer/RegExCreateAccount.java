package RegExServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RegExCreateAccount extends RegExPage {
    /**
     *
     */
    private String error;

    /**
     * URI of this page's base HTML file.
     */
    private static String pageURI = RegExHttpHandler.DOCUMENT_ROOT + "/create-account/index.html";

    /**
     * If there is no error to be displayed on the home page, this constant can be used
     * to identify that.
     */
    public static String NO_ERROR = null;

    /**
     *
     * @param error
     */
    public RegExCreateAccount(String error) {
        this.error = error;
    }

    /**
     *
     * @return
     * @throws IOException
     */
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
