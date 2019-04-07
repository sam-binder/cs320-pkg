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
     * If not applicable, strings should be blank, not null
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
    /*
     * Method to enter customer data
     */
    public void enterCustomerData(){

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
