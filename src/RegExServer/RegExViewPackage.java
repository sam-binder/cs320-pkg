package RegExServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RegExViewPackage extends RegExPage {
    /**
     * URI of this page's base HTML file.
     */
    private static String pageURI = RegExHttpHandler.DOCUMENT_ROOT + "/view-package/index.html";

    /**
     * If no packageID was specified on page load, a single "enter tracking ID" input is shown
     */
    private static String noPackageIDURI = RegExHttpHandler.DOCUMENT_ROOT + "/view-package/no-package-id.part";

    /**
     * The URI to the navbar links that will only be dropped in if the user is logged in.
     */
    private static String navbarLinksURI = RegExHttpHandler.DOCUMENT_ROOT + "/view-package/navbar-links.part";

    /**
     * If a packageID was specified, this will show the package details
     */
    private static String packageIDURI = RegExHttpHandler.DOCUMENT_ROOT + "/view-package/specified-package-id.part";

    /**
     * The packageID being tracked.
     */
    private String packageID;

    /**
     * Will be true if the user has a session, else false
     */
    private boolean isLoggedIn;

    /**
     * Constructs a new RegExViewPackage page with the package tracking ID of packageID.
     *
     * @param packageID  The package ID being tracked.
     */
    public RegExViewPackage(String packageID, boolean isLoggedIn) {
        this.packageID = packageID;
        this.isLoggedIn = isLoggedIn;
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

        if(this.packageID == null) {
            // generate a single input box asking for the package ID
            pageContent = pageContent.replace(
                "@{package-title}",
                "View Package Details"
            ).replace(
            "@{view-package-main-content}",
                new String(
                    Files.readAllBytes(
                        Paths.get(noPackageIDURI)
                    ),
                    StandardCharsets.UTF_8
                )
            );
        } else {
            // we have a specified packageID, we need to run some things on that
            String specifiedIDPageContent = new String(
                Files.readAllBytes(
                    Paths.get(packageIDURI)
                ),
                StandardCharsets.UTF_8
            );

            // request from H2 the package details

            // generate the package details table
            pageContent = pageContent.replace(
                "@{package-title}",
                this.packageID
            ).replace(
                "@{view-package-main-content}",
                specifiedIDPageContent
            );
        }

        // drops in the navbar links if the user is logged in
        pageContent = pageContent.replace(
            "@{logged-in-navbar-links}",
            getNavbarLinks()
        );

        // return our page content as bytes
        return pageContent.getBytes();
    }


    private String getNavbarLinks() throws IOException {
        // if the user is logged in, return the navbar links (and toggler)
        if(this.isLoggedIn) {
            return new String(
                Files.readAllBytes(
                    Paths.get(navbarLinksURI)
                ),
                StandardCharsets.UTF_8
            );
        // else return an empty string
        } else {
            return "";
        }
    }
}
