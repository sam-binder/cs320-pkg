package RegExModel;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

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

    public ResultSet createNewAddress(String company, String attention, String streetLine1, String streetLine2,
                                      String zip_ID_fk, String account_number_fk) throws SQLException {

        ResultSet does_it_exist = retrieveAddress(company, attention, streetLine1, streetLine2, zip_ID_fk, account_number_fk);
        if (!does_it_exist.next()){
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
