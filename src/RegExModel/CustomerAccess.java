package RegExModel;
import java.sql.*;

/**
 * Created by Walter Schaertl on 3/24/2019.
 */

/**
 * Class that contains ease of use methods for the customers that use the system.
 * Functions here are called by the server.
 */
public class CustomerAccess implements AutoCloseable{
    private String username;
    private Connection connection;
    private H2Access h2;

    /**
     * How an customer logs in
     * @param username the customer's username
     * @param password the customer's password
     * @throws SQLException If the username/password is incorrect, an exception is raised
     */
    public CustomerAccess(String username, String password) throws SQLException {
        this.username = username;
        this.h2 = new H2Access();
        this.connection = H2Access.createConnection(username, password);
    }

    /**
     * Sets up a new billing situation for a customer. By default, the balance starts at zero,
     * and the pay model is monthly, and the employee foreign key is null. If this user
     * become prolific or wants to change values, they call in, an accounting employee will
     * be assigned to them, and thet employee will update their billing.
     * @return boolean true if the billing was set up correctly
     */
    public boolean setUpBillingInfo() {
        // Zero balance, monthly payments, account number, no employee assigned
        String billingQuery = "INSERT INTO billing(balance_to_date, pay_model, " +
                "account_number_fk, employee_id) VALUES(0, 'monthly', %d, null);";
        // Query to update the billing_fk of the customer
        String customerQuery = "UPDATE customer SET billing_fk=%d WHERE account_number=%d";
        // Get the account number fk (which is the general fk of the user)
        int accountNumber = H2Access.getUserFK(username);
        // The billing ID, to be associated with the customer once it's created
        int billingID = 0;
        try {
            // While this function sets up customer billing, the database access is not
            // done with a customer access
            Connection conn = H2Access.createConnection("me", "password");
            String formattedQuery = String.format(billingQuery, accountNumber);
            PreparedStatement prep = conn.prepareStatement(formattedQuery, Statement.RETURN_GENERATED_KEYS);
            prep.executeUpdate();
            ResultSet keys = prep.getGeneratedKeys();
            if (keys.next())
                billingID = keys.getInt("ID");

            // Update the customer with the fk to this new billing object
            formattedQuery = String.format(customerQuery, billingID, accountNumber);
            conn.createStatement().execute(formattedQuery);
        } catch (SQLException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Allows the user to enter their basic information.
     *
     * @param firstName The customer's first name
     * @param lastName The customer's last name
     * @param phoneNumber The customer's phone number
     * @return a boolean, true if the update was successful, else false.
     */
    public boolean enterBasicInformation(String firstName, String lastName, String phoneNumber){
        String query = String.format("UPDATE customer SET first_name='%s', " +
                "last_name='%s', phone_no='%s'", firstName, lastName, phoneNumber);
        return H2Access.createAndExecute(connection, query);
    }

    /**
     * Lets the User enter in minimal information about their address. All foreign keys
     * are generated/looked up in the process. If not applicable, strings should be blank, not null
     * Error codes:
     *  0   Success
     *  -1  Could not get user general purpose foreignKey
     *  -2  ZipCode doesn't exists
     *  -3  An unexpected SQL exception occurred.
     * @param company The company name
     * @param attention The name of the individual
     * @param streetLineOne The first address line
     * @param streetLineTwo The second address line
     * @param zipCode The 5 digit zip code
     * @return an error code, 0 for success
     */
    public int enterAddress(String company, String attention, String streetLineOne,
                            String streetLineTwo, int zipCode){
        String zipQuery = "SELECT id FROM zip_code WHERE zip_code = " + zipCode + ";";
        String addressQuery = "INSERT INTO " +
                "address(company, attn, street_line_1, street_line_2, zip_fk, account_number_fk) " +
                "VALUES('%s', '%s', '%s', '%s', %d, %d);";
        String customerQuery = "UPDATE customer SET mailing_address_id_fk=%d WHERE account_number=%d";
        ResultSet r;
        int zipFK;
        int addressID = 0;

        // Get the user's fk
        int userFK = H2Access.getUserFK(username);
        if(userFK == -1)
            return -1;

        try {
            // Get the ZIP ID fk based on the zipCode
            r = connection.createStatement().executeQuery(zipQuery);
            if(!r.next())
                return -2;
            else
                zipFK = r.getInt(1);

            // Create the entry in the address table
            addressQuery = String.format(addressQuery, company, attention,
                    streetLineOne, streetLineTwo, zipFK, userFK);
            PreparedStatement prep = connection.prepareStatement(addressQuery, Statement.RETURN_GENERATED_KEYS);
            prep.executeUpdate();
            ResultSet keys = prep.getGeneratedKeys();
            if (keys.next())
                addressID = keys.getInt("ID");

            // Set this Id as the users main address
            connection.createStatement().execute(String.format(customerQuery, addressID, userFK));
            return 0;
        } catch (SQLException e){e.printStackTrace();}
        return -3;
    }

    /**
     * Call this one when adding an address
     * @param company
     * @param attention
     * @param streetLine1
     * @param streetLine2
     * @param zip_ID_fk
     * @param account_number_fk
     * @return
     * @throws SQLException
     */
    public ResultSet createNewAddress(String company, String attention, String streetLine1, String streetLine2,
                                      String zip_ID_fk, String account_number_fk) throws SQLException {

        ResultSet does_it_exist = retrieveAddress(company, attention, streetLine1, streetLine2, zip_ID_fk, account_number_fk);
        if (!does_it_exist.next()){ // this is how you check if its an empty ResultSet, per stack overflow
            does_it_exist = createAddress(company, attention, streetLine1, streetLine2, zip_ID_fk, account_number_fk);
            does_it_exist = retrieveAddress(company, attention, streetLine1, streetLine2, zip_ID_fk, account_number_fk);
        }

        return does_it_exist;

    }

    private ResultSet retrieveAddress(String company, String attention, String streetLine1, String streetLine2,
                                      String zip_ID_fk, String account_number_fk){
        int zip_ID_fk_numeric = Integer.parseInt(zip_ID_fk);
        int account_number_fk_numeric = Integer.parseInt(account_number_fk);
        String query =
                "SELECT * FROM address "+
                "WHERE (COMPANY = '" + company + "') AND " +
                "(ATTN = '" + attention +"') AND " +
                "(STREET_LINE_1 = '" + streetLine1 + "') AND " +
                "(STREET_LINE_2 = '" + streetLine2 + "') AND " +
                "(ZIP_ID_FK = "+ zip_ID_fk_numeric + ") AND " +
                "(ACCOUNT_NUMBER_FK = " + account_number_fk_numeric + ")";



        return h2.createAndExecuteQuery(connection, query);
    }
    /**
     * create a new address for a customer. Called after querying whether this address already exists.
     * @param company
     * @param attention
     * @param streetLine1
     * @param streetLine2
     * @param zip_ID_fk
     * @param account_number_fk
     * @return the result of the query, which should be empty.
     */
    private ResultSet createAddress(String company, String attention, String streetLine1, String streetLine2,
                              String zip_ID_fk, String account_number_fk){
        int zip_ID_fk_numeric = Integer.parseInt(zip_ID_fk);
        int account_number_fk_numeric = Integer.parseInt(account_number_fk);
        String query =
                "INSERT INTO address "+
                "(COMPANY, ATTN, STREET_LINE_1, STREET_LINE_2, ZIP_ID_FK, ACCOUNT_NUMBER_FK)" +
                " VALUES(" +
                company + ", " +
                attention + ", " +
                streetLine1 + ", " +
                streetLine2 + ", " +
                zip_ID_fk_numeric + ", " +
                account_number_fk_numeric + ")";



        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * Gets all addresses in customer's "rolodex", that they've ever sent packages from or to
     * @param account_number_fk
     * @return ResultSet of all addresses with their account number associated
     */
    public ResultSet getAllAddresses(String account_number_fk){
        int account_number_fk_numeric = Integer.parseInt(account_number_fk);
        String query = "SELECT * FROM address " + "WHERE (ACCOUNT_NUMBER_FK = " + account_number_fk_numeric + ")";
        return h2.createAndExecuteQuery(connection, query);
    }


    //Stubbed out method for future functionality
    public void sendPackage(){

    }

    //Stubbed out method for future functionality
    public void addMoneyToAccount(){

    }


    /**
     * Function required to be implemented by AutoCloseable.
     * Lets CustomerAccess be used in a 'try with resources' block
     */
    @Override
    public void close(){
        h2.closeConnection(this.connection);
    }
}
