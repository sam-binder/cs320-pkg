package RegExServer;

import RegExModel.CustomerAccess;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;

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
     * @param userRegExSession The session to render the page for.
     */
    public RegExAccountInfo(RegExSession userRegExSession) {
        this.userRegExSession = userRegExSession;
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

        // dumps in all of the session values
        pageContent = this.userRegExSession.replaceVarPlaceholders(pageContent);

        // next we have to replace all of the editable content
        try {
            // creates a customer access
            CustomerAccess tempCustomerAccess = new CustomerAccess(
                    this.userRegExSession.userName,
                    this.userRegExSession.password
            );

            // gets the user's information
            ResultSet userDetails = tempCustomerAccess.getUserInformation();

            // if there exists userDetails next row (it always will)
            if (userDetails.next()) {
                // replace all of the information we just grabbed
                pageContent = pageContent.replace(
                        "@{first-name}",
                        userDetails.getString(6)
                ).replace(
                        "@{last-name}",
                        userDetails.getString(7)
                ).replace(
                        "@{phone-number}",
                        userDetails.getString(8)
                ).replace(
                        "@{company}",
                        userDetails.getString(10)
                ).replace(
                        "@{attn}",
                        userDetails.getString(11)
                ).replace(
                        "@{address-line-1}",
                        userDetails.getString(12)
                ).replace(
                        "@{address-line-2}",
                        userDetails.getString(13)
                ).replace(
                        "@{city}",
                        userDetails.getString(17)
                ).replace(
                        "@{state}",
                        userDetails.getString(18)
                ).replace(
                        "@{zip}",
                        userDetails.getString(16)
                );

                // last but not least we have to drop in the packages table
                ResultSet sentPackages = tempCustomerAccess.getSentPackages();

                // builds our sent packages table
                StringBuilder sentPackagesTable = new StringBuilder();
                // if there were no packages
                if (!sentPackages.next()) {
                    // report that here
                    pageContent = pageContent.replace(
                        "@{sent-packages-table}",
                        "<tr><td class='text-bold text-center text-italic'>No packages yet.</td></tr>"
                    );
                } else {
                    int count = 0;
                    do {
                        // start a new row if we are on an even 3
                        if (count % 3 == 0) {
                            sentPackagesTable.append("<tr>");
                        }

                        // generates the package tracking ID
                        String trackingID = generateTrackingID(
                            sentPackages.getInt(1),
                            sentPackages.getInt(2),
                            sentPackages.getString(3)
                        );


                        // append the link to this package
                        sentPackagesTable.append(
                                "<td>" +
                                    "<a href='/view-package/?package-id='" + trackingID + "'>" + trackingID + "</a>" +
                                "</td>"
                        );

                        // increments count
                        ++count;

                        // closes the row if we need to
                        if (count % 3 == 0) {
                            sentPackagesTable.append("</tr>");
                        }
                    } while (sentPackages.next());

                    // adds a closing row if it was not done in the loop
                    if (count % 3 != 0) {
                        sentPackagesTable.append("</tr>");
                    }

                    // throw it into the table
                    pageContent = pageContent.replace(
                            "@{sent-packages-table}",
                            sentPackagesTable.toString()
                    );
                }
            }
        } catch (SQLException sqle) {
            /* most likely this will never happen */
        }

        // returns the byte-level form of the page content
        return pageContent.getBytes();
    }

    private String generateTrackingID(int accountID, int serviceID, String packageSerial) {
        // the trackingID stringbuilder
        StringBuilder trackingID = new StringBuilder();

        trackingID.append(String.format("%06d", accountID));
        trackingID.append(String.format("%02d", serviceID));
        trackingID.append(packageSerial);


        // our total sum will be made using this variable
        int sum = 0;

        for (char c : trackingID.toString().toCharArray()) {
            // add the ascii value of the char at index i
            sum += c;
        }

        // appends the checkBit
        trackingID.append((char) ((sum % 17) + 74));

        // returns true if the check digit matches the sum mod 17
        return trackingID.toString();
    }
}