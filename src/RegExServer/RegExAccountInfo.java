package RegExServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RegExAccountInfo extends RegExPage {
    /**
     * URI of this page's base HTML file.
     */
    private static String pageURI = RegExHttpHandler.DOCUMENT_ROOT + "/account-info/index.html";

    /**
     * The session this page should be generated for
     */
    private RegExSession userRegExSession;

    /**
     * Creates a new RegExAccountInfo page
     *
     * @param userRegExSession  The session to render the page for.
     */
    public RegExAccountInfo(RegExSession userRegExSession) {
        this.userRegExSession = userRegExSession;
    }

    /**
     * Returns the byte-level form of the page's content.
     *
     * @return  The byte-level form of the page's content.
     * @throws IOException  If any IOException is encountered, it is thrown out to the caller.
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

        // dumps in all of the session values
        pageContent = this.userRegExSession.replaceVarPlaceholders(pageContent);

        // next we have to replace all of the editable content

        // returns the byte-level form of the page content
        return pageContent.getBytes();
    }
}
