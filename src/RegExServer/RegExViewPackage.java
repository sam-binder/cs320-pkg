package RegExServer;

// FILE: RegExViewPackage.java

import RegExModel.H2Access;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * RegExPage class used to format the ViewPackage page.
 *
 * @author Kevin J. Becker (kjb2503)
 * @version 03/20/2019
 */
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
                accntNum = Integer.parseInt(this.packageID.substring(0, 6));
                serial = this.packageID.substring(8, 14);
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

                // queries the DB for tracking information
                ResultSet packageTrackingInfo = H2Access.trackPackage(accntNum, serial);
                // queries the DB for package data
                ResultSet packageStats = H2Access.viewPackageData(accntNum, serial);
                try {
                    if(packageStats != null && packageStats.next()) {
                        // load in the success section content
                        specifiedIDPageContent = new String(
                            Files.readAllBytes(
                                Paths.get(packageIDSucceedURI)
                            ),
                            StandardCharsets.UTF_8
                        ).replace(
                            "@{package-height}",
                            packageStats.getString(4)
                        ).replace(
                            "@{package-length}",
                            packageStats.getString(5)
                        ).replace(
                            "@{package-width}",
                            packageStats.getString(6)
                        ).replace(
                            "@{package-weight}",
                            packageStats.getString(7)
                        );

                        // will eventually be the table of tracking updates
                        StringBuilder trackingTable = new StringBuilder();

                        if(!packageTrackingInfo.next()) {
                            // sets that there were no entries returned
                            trackingTable.append(
                                "<tr>" +
                                    "<td class='text-italic text-bold text-center' colspan='3'>No tracking data yet.</td>" +
                                "</tr>"
                            );
                        } else {
                            // next it goes through and dump in the table of tracking updates
                            do {
                                trackingTable.append(
                                    generateTableRow(
                                        packageTrackingInfo.getString(2),
                                        packageTrackingInfo.getString(3),
                                        packageTrackingInfo.getString(8),
                                        packageTrackingInfo.getString(9),
                                        packageTrackingInfo.getString(5)
                                    )
                                );
                            } while(packageTrackingInfo.next());
                        }

                        // drops in our tracking table entries
                        specifiedIDPageContent = specifiedIDPageContent.replace(
                            "@{tracking-table}",
                            trackingTable
                        );
                    } else {
                        // if we get here the database returned a null when querying information about the package
                        // implying it doesn't exist
                        specifiedIDPageContent = new String(
                            Files.readAllBytes(
                                Paths.get(packageIDFailURI)
                            ),
                            StandardCharsets.UTF_8
                        ).replace(
                            "@{error-dialog}",
                            RegExDialog.getErrorDialogHTML("Package does not exist.")
                        ).replace(
                            "@{entered-package-id}",
                            this.packageID
                        );
                    }
                } catch (SQLException sqle) {
                    // if we get here the database returned a null when querying information about the package
                    // implying it doesn't exist
                    specifiedIDPageContent = new String(
                        Files.readAllBytes(
                            Paths.get(packageIDFailURI)
                        ),
                        StandardCharsets.UTF_8
                    ).replace(
                        "@{error-dialog}",
                        RegExDialog.getErrorDialogHTML(
                            "An error was encountered when trying to get package data. Please try again in a little bit."
                        )
                    ).replace(
                        "@{entered-package-id}",
                        this.packageID
                    );
                }
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
                    RegExDialog.getErrorDialogHTML(errorMsg)
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

    /**
     * Returns the navbar links if the user is signed in, else an empty string.
     *
     * @return  The navbar links if the user is signed in, else an empty string.
     * @throws IOException  If any IOException is encountered, it is thrown out to the calling method.
     */
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

    /**
     * Determines if the tracking ID is valid (checksum is correct)
     *
     * @return True if the checksum passes. False otherwise.
     */
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

    /**
     * Generates a row for the package tracking table.
     *
     * @param dateStr  The string representation of the date.
     * @param timeStr  The string representation of the time.
     * @param city  The city the update took place in.
     * @param state  The state the update took place in.
     * @param transactionID  The transaction ID to print a note about the update with.
     *
     * @return  A row for the package tracking table.
     */
    private String generateTableRow(String dateStr, String timeStr, String city, String state, String transactionID) {
        // a formatter to ensure consistent dates
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date = new Date();
        DateFormat dateFormatOutput = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);

        // a formatter to ensure consistent TIMES
        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        Date time = new Date();
        DateFormat timeFormatOutput = new SimpleDateFormat("hh:mm a");

        try {
            // parses date and time
            date = dateFormat.parse(dateStr);
            time = timeFormat.parse(timeStr);
        } catch (ParseException pe) {
            /* this will never happen */
        }

        // String for the location
        String location = " ";

        String note = "";
        switch(transactionID.charAt(0)) {
            case 'V':
                location ="In Transit";
                note = "Vehicle loading scan.";
                break;
            case 'T':
                if(city != null && state != null)
                    location = city + ", " + state;
                switch(transactionID.charAt(1)) {
                    case 'O':
                        note = "Arrived at carrier facility.";
                        break;
                    case 'D':
                        note = "Arrived at destination.";
                }
                break;
            case 'H':
                note = "Transfer at package facility.";
                if(city != null && state != null)
                    location = city + ", " + state;
                break;
        }

        // appends the transaction ID to the end of the note
        note += " (" + transactionID + ")";



        // returns the update as a table row
        return "<tr>" +
                    "<td>" + dateFormatOutput.format(date) + " at " + timeFormatOutput.format(time) + "</td>" +
                    "<td class='text-bold'>" + location + "</td>" +
                    "<td>" + note + "</td>" +
                "</tr>";
    }
}
