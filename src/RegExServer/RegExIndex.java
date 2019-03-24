package RegExServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RegExIndex extends RegExPage {
    // the URI for this page
    private static String pageURI = RegExHttpHandler.DOCUMENT_ROOT + "/index.html";

    public static String NO_ERROR = null;

    // the error message to print
    private String error;

    public RegExIndex(String error) {
        this.error = error;
    }

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
