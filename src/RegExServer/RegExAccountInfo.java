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
        try {
            // creates a customer access
            CustomerAccess tempCustomerAccess = new CustomerAccess(
                this.userRegExSession.userName,
                this.userRegExSession.password
            );

            // gets the user's information
            ResultSet userDetails = tempCustomerAccess.getUserInformation();


            // if there exists userDetails next row (it always will)
            if(userDetails.next()) {
                // replace all of the information we just grabbed
                pageContent = pageContent.replace(
                    "@{first-name}",
                    userDetails.getString(6)
                ).replace (
                    "@{last-name}",
                    userDetails.getString(7)
                ).replace(
                    "@{phone-number}",
                    userDetails.getString(8)
                );
            }
        } catch (SQLException sqle) {
            sqle.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        // returns the byte-level form of the page content
        return pageContent.getBytes();
    }
}
