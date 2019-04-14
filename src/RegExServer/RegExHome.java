package RegExServer;

// FILE: RegExHome.java

import RegExModel.CustomerAccess;
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

        // we have to build out "last 3 transactions" table
        StringBuilder lastThreeTransTable = new StringBuilder();

        // attempts to load in all of the three things
        try {
            CustomerAccess tempCustomerAccess = new CustomerAccess(
                this.userRegExSession.userName,
                this.userRegExSession.password
            );

            // gets the resultset for the last (up to) three transactions on this account
            ResultSet lastThreeTrans = tempCustomerAccess.getLastThreeTransactions(
                this.userRegExSession.accountNumber
            );

            // if the ResultSet has actual data
            if (lastThreeTrans != null) {
                // load up the first transaction if it exists
                if (lastThreeTrans.next()) {
                    do {
                        lastThreeTransTable.append(
                            generateTableRow(
                                RegExModel.Util.generateTrackingID(
                                    lastThreeTrans.getInt(3),
                                    lastThreeTrans.getInt(6),
                                    lastThreeTrans.getString(4)
                                ),
                                lastThreeTrans.getString(5),
                                lastThreeTrans.getString(1),
                                lastThreeTrans.getString(2)
                            )
                        );
                    } while (lastThreeTrans.next());
                } else {
                    // dump in a "no records yet"
                    lastThreeTransTable.append(
                        "<tr>" +
                            "<td colspan='3' class='text-italic text-bold text-center'>" +
                                "No transactions yet." +
                            "</td>" +
                        "</tr>"
                    );
                }
            }
        } catch (SQLException sqle) {
            /* if this happens we have a bigger issue */
        }

        // places our table content
        pageContent = pageContent.replace(
            "@{last-three-transactions}",
            lastThreeTransTable
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

    /**
     * Generates a row for the package tracking table.
     *
     * @param transactionID The transaction ID to print a note about the update with.
     * @param dateStr       The string representation of the date.
     * @param timeStr       The string representation of the time.
     * @return A row for the package tracking table.
     */
    private String generateTableRow(String trackingID, String transactionID, String dateStr, String timeStr) {
        // a formatter to ensure consistent dates
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date date = new Date();
        DateFormat dateFormatOutput = new SimpleDateFormat("MMMM dd, yyyy", Locale.ENGLISH);

        // a formatter to ensure consistent TIMES
        DateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH);
        Date time = new Date();
        DateFormat timeFormatOutput = new SimpleDateFormat("hh:mm a");

        // parses date and time
        try {
            date = dateFormat.parse(dateStr);
            time = timeFormat.parse(timeStr);
        } catch (ParseException pe) {
            /* this will never happen */
        }

        // generates the note
        String note = "";
        switch (transactionID.charAt(0)) {
            case 'V':
            case 'H':
                note = "In transit";
                break;
            case 'T':
                switch(transactionID.charAt(1)) {
                    case 'O':
                        note = "Left Carrier Facility";
                        break;
                    case 'D':
                        note = "Delivered";
                        break;
                }
                break;
        }

        // puts the last transaction location ID after the note
        note += " (" + transactionID + ")";

        // returns the update as a table row
        return "<tr>" +
                    "<td>" +
                        "<a href='/view-package/?package-id=" + trackingID + "' title='View more info about package " + trackingID + ".'>" +
                            trackingID +
                        "</a>" +
                    "</td>" +
                    "<td>" + note + "</td>" +
                    "<td>" + dateFormatOutput.format(date) + " at " + timeFormatOutput.format(time) + "</td>" +
                "</tr>";
    }
}
