package RegExServer;

// FILE: RegExHome.java

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * RegEx application home page generator.
 *
 * @author Kevin J. Becker (kjb2503)
 * @version 03/24/2019
 */
public class RegExHome extends RegExPage{
    /**
     * URI of this page's base HTML file.
     */
    private static String pageURI = RegExHttpHandler.DOCUMENT_ROOT + "/home/index.html";

    /**
     * The session this page represents.
     */
    private RegExSession userRegExSession;

    /**
     * Constructs a new RegExHome page with the session of userRegExSession.
     *
     * @param userRegExSession  The session to render with.
     */
    public RegExHome(RegExSession userRegExSession) {
        this.userRegExSession = userRegExSession;
    }

    /**
     * Gets the byte form of the page's full content.
     *
     * @return  The byte-level form of the home page's content.
     * @throws IOException  If any IOException is encountered, it is thrown to the caller.
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

        // replaces all var placeholders with session details
        pageContent = userRegExSession.replaceVarPlaceholders(pageContent);

        // return our page content as bytes
        return pageContent.getBytes();
    }
}
