package RegExServer;

// FILE: RegExHome.java

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

        // we have to build out "last 3 transactions" table
        StringBuilder lastThreeTransTable = new StringBuilder();

        // gets the resultset for the last (up to) three transactions on this account
        ResultSet lastThreeTrans = H2Access.getLastThreeTransactions(this.userRegExSession.accountNumber);

        // attempts to load in all of the three things
        try {
            // if the resultset has actual data
            if(lastThreeTrans != null) {
                // load up the first transaction if it exists
                if(lastThreeTrans.next()) {
                    do {
                        lastThreeTransTable.append(
                                generateTableRow(
                                        generateTrackingID(
                                                131,
                                                lastThreeTrans.getString(6),
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
            /* hopefully this never happens */
        }

        // places our table content
        pageContent = pageContent.replace(
            "@{last-three-transactions}",
            lastThreeTransTable
        );

        // replaces all var placeholders with session details
        pageContent = userRegExSession.replaceVarPlaceholders(pageContent);

        // return our page content as bytes
        return pageContent.getBytes();
    }

    /**
     * Generates a row for the package tracking table.
     *
     * @param transactionID  The transaction ID to print a note about the update with.
     * @param dateStr  The string representation of the date.
     * @param timeStr  The string representation of the time.
     *
     * @return  A row for the package tracking table.
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

        try {
            // parses date and time
            date = dateFormat.parse(dateStr);
            time = timeFormat.parse(timeStr);
        } catch (ParseException pe) {
            /* this will never happen */
        }

        String note = "";
        switch(transactionID.charAt(0)) {
            case 'V':
            case 'H':
                note = "In transit";
                break;
            case 'T':
                note = "Delivered";
                break;
        }

        note += " (" + transactionID + ")";

        // returns the update as a table row
        return "<tr>" +
                    "<td>" +
                        "<a href='/view-package/?package-id=" + trackingID + "' title='View more info about package "+ trackingID +".'>" +
                            trackingID +
                        "</a>" +
                    "</td>" +
                    "<td>" + note + "</td>" +
                    "<td>" + dateFormatOutput.format(date) + " at " + timeFormatOutput.format(time) + "</td>" +
                "</tr>";
    }

    private String generateTrackingID(int accountID, String serviceID, String packageSerial) {
        // the trackingID stringbuilder
        StringBuilder trackingID = new StringBuilder();

        trackingID.append(String.format("%06d", accountID));
        trackingID.append(serviceID);
        trackingID.append(packageSerial);


        // our total sum will be made using this variable
        int sum = 0;

        for(char c : trackingID.toString().toCharArray()) {
            // add the ascii value of the char at index i
            sum += c;
        }

        // appends the checkBit
        trackingID.append((char)((sum % 17) + 74));

        // returns true if the check digit matches the sum mod 17
        return trackingID.toString();
    }
}
