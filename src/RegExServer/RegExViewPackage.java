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
     * If a packageID was specified AND THE ID IS REAL, this will show the package details.
     */
    private static String packageIDSucceedURI = RegExHttpHandler.DOCUMENT_ROOT + "/view-package/specified-package-id-success.part";

    /**
     * If a packageID was specified BUT THE PACKAGE IS NOT REAL, this will show an error dialog saying so.
     */
    private static String packageIDFailURI = RegExHttpHandler.DOCUMENT_ROOT + "/view-package/specified-package-id-failure.part";

    /**
     * The URI to the navbar links that will only be dropped in if the user is logged in.
     */
    private static String navbarLinksURI = RegExHttpHandler.DOCUMENT_ROOT + "/view-package/navbar-links.part";

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
            // assume the parsing of package ID works by default (only if the length is 16)
            boolean parseSucceeded = this.packageID.length() == 15;
            // the account number starts at -1 and will be updated
            int accntNum = -1;
            String serial = "";

            // parse out account number (first 6 digits of tracking ID)
            try {
                accntNum = Integer.parseInt(this.packageID.substring(0, 5));
                serial = this.packageID.substring(8, 13);
            // any thrown exception will mean the parse did not succeed
            } catch (Exception e) {
                // log that an error has occurred where the package ID was not valid
                RegExLogger.error("requested package ID is not valid", 1);
                // if we make it here the parse did not succeed
                parseSucceeded = false;
            }

            // the page content relating to the specific ID
            String specifiedIDPageContent;

            // request from H2 the package details if the parse succeeded
            if(parseSucceeded && trackingIDIsValid()) {
                // logs that the package information is being requested from the DB
                RegExLogger.log("requesting information from DB about package " + this.packageID, 1);

                // load in the success section content
                specifiedIDPageContent = new String(
                    Files.readAllBytes(
                        Paths.get(packageIDSucceedURI)
                    ),
                    StandardCharsets.UTF_8
                );

            } else {
                // load in the failure section content
                specifiedIDPageContent = new String(
                    Files.readAllBytes(
                        Paths.get(packageIDFailURI)
                    ),
                    StandardCharsets.UTF_8
                );

                // the error message to include in the error dialog
                String errorMsg;
                // attempts to diagnose the error that occurred
                if(this.packageID.length() != 15) {
                    errorMsg = "Tracking ID must be 15 characters in length.";
                } else if(!trackingIDIsValid()) {
                    errorMsg = this.packageID + " is not a valid tracking ID.";
                } else {
                    errorMsg = "An unknown error occurred.";
                }

                // puts in place our error dialog
                specifiedIDPageContent = specifiedIDPageContent.replace(
                    "@{error-dialog}",
                    RegExErrorDialog.getErrorDialogHTML(errorMsg)
                ).replace(
                    "@{entered-package-id}",
                    this.packageID
                );
            }


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

    private boolean trackingIDIsValid() {
        // pulls out the check digit (the last character)
        char checkDigit = this.packageID.charAt(this.packageID.length() - 1);

        // our total sum will be made using this variable
        int sum = 0;
        // goes through each of the non check-digit characters
        for(int i = 0; i < 14; ++i) {
            // add the ascii value of the char at index i
            sum += this.packageID.charAt(i);
        }

        // returns true if the check digit matches the sum mod 17
        return (sum % 17) + 74 == checkDigit;
    }
}
