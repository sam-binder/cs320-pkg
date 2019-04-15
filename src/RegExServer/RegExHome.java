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
public class RegExHome extends RegExPage {
    /**
     * URI of this page's base HTML file.
     */
    private static String pageURI = RegExHttpHandler.DOCUMENT_ROOT + "/home/index.html";
    /**
     * The session this page represents.
     */
    private RegExSession userRegExSession;

    /**
     * A public "NO SENT MESSAGE" variable to use when generating just the basic home page.
     */
    public static final String NO_TRACKING_ID = null;

    /**
     * The message that is displayed when a user has just sent a package and is redirected back to home.
     */
    private String trackingID;

    /**
     * Constructs a new RegExHome page with the session of userRegExSession.
     *
     * @param userRegExSession The session to render with.
     * @param trackingID The ID to display as a message.
     */
    public RegExHome(RegExSession userRegExSession, String trackingID) {
        this.userRegExSession = userRegExSession;
        this.trackingID = trackingID;
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

        // replaces all var placeholders with session details
        pageContent = userRegExSession.replaceVarPlaceholders(pageContent);

        // lastly goes drops in our success message if there was a successfully sent package
        pageContent = pageContent.replace(
            "@{tracking-id-message}",
            (this.trackingID != null) ?
                RegExDialog.getSuccessDialogHTML(
                    "Your package has successfully been created. " +
                    "Track it <a href='/view-package/?package-id=" + this.trackingID + "'>here</a>."
                ) : ""
        );

        // return our page content as bytes
        return pageContent.getBytes();
    }
}
