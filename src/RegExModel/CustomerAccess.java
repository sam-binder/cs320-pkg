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
        this.connection = this.h2.createConnection(username, password);
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
        int userFK = h2.getUserFK(username);
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
     * Ease of use function to view data about a package.
     * @param accntNum int: The account number of who order the package
     * @param serial string: The package's serial number
     * @return A ResultSet of data about the package, to be processed by the client
     */
    public ResultSet viewPackageData(int accntNum, String serial){
        String query = "SELECT * from package WHERE account_number_fk=" + accntNum +
                " AND serial='" + serial +"'";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * Ease of use function to view the history of a package.
     * @param accntNum int: The account number of who order the package
     * @param serial string: The package's serial number
     * @return A ResultSet of data about the package's history, to be processed by the client
     */
    public ResultSet trackPackage(int accntNum, String serial){
        String query = "SELECT * from transaction WHERE account_number_fk=" + accntNum +
                " AND package_serial_fk='" + serial +"'";
        return h2.createAndExecuteQuery(connection, query);
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
