package RegExModel;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.SQLException;

/**
 * Created by Walter Schaertl on 3/23/2019.
 */

/**
 * Class that contains ease of use methods for the employees that use the system.
 * Functions here are called by the server.
 */
public class EmployeeAccess implements AutoCloseable{
    private String username, type;
    private Connection connection;
    private H2Access h2;

    /**
     * How an employee logs in
     * @param username the employee's username
     * @param password the employee's password
     * @param type the type of employee (accounting or package)
     * @throws SQLException Indicates a failure to log in, either wrong username or password
     */
    public EmployeeAccess(String username, String password, String type) throws SQLException{
        this.username = username;
        this.type = type;
        this.h2 = new H2Access();
        this.connection = this.h2.createConnection(username, password);
    }

    /**
     * An easy way to get the ID of a logged in employee
     * @return int: The employee's ID. returns -1 on failure.
     */
    public int getId() {
        String query = "SELECT general_fk FROM user where username='" + username + "'";
        ResultSet r = h2.createAndExecuteQuery(connection, query);
        try {
            if (r.next())
                return r.getInt(1);
        }catch (SQLException e) {}
        return -1;
    }

    /**
     * Function required to be implemented by AutoCloseable.
     * Lets CustomerAccess be used in a 'try with resources' block
     */
    @Override
    public void close(){
        h2.closeConnection(this.connection);
    }

    /**
     * Ease of use function that lets logged in package employees update a packages location.
     * Makes an entry in the transaction table.
     *
     * @param locationId The location the package employee is at
     * @param acctNumber The account number assocaited with the package
     * @param pkgSerial The package serial
     */
    public void updatePackageLocation(String locationId, int acctNumber, String pkgSerial){
        Date date = new Date(System.currentTimeMillis());
        Time time = new Time(System.currentTimeMillis());
        int employeeId = getId();
        String query = String.format("INSERT INTO transaction(DATE, TIME, EMPLOYEE_ID_FK, LOCATION_ID_FK, " +
                        "ACCOUNT_NUMBER_FK, PACKAGE_SERIAL_FK) VALUES (\'%s\',\'%s\',\'%s\',\'%s\',\'%s\',\'%s\');",
                        date, time, employeeId, locationId, acctNumber, pkgSerial);
        h2.createAndExecute(connection, query);
    }

    /**
     * An easy way for accounting employees to see all the customers in the system to
     * better aid them with billing or questions.
     * @return a ResultSet of customers, to be processed by the client
     */
    public ResultSet getCustomers(){
        String query = "SELECT * from customer";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * An easy way for accounting employees to see all the customers in the system,
     * and allows for selection and trimming down of results.
     * @return a ResultSet of customers, to be processed by the client
     */
    public ResultSet getCustomersWhere(String conditional){
        String query = "SELECT * from customer WHERE " +  conditional;
        return h2.createAndExecuteQuery(connection, query);
}

    /**
     * An easy way for accounting employees to see customer billing information
     * @param acctNumber int: The id of the account to view the billing information
     * @return a ResultSet of customer billing information, to be processed by the client
     */
    public ResultSet viewCustomerBilling(int acctNumber){
        String query = "SELECT * from billing WHERE account_number_fk=" + acctNumber;
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * An easy way for accounting employee's to track a package
     * @param accntNum: The account id associate with the package
     * @param serial: The package's serial
     * @return A ResultSet of transactions showing the locations a package has been
     */
    public ResultSet viewPackageHistory(int accntNum, String serial){
        String query = "SELECT * from transaction WHERE account_number_fk=" + accntNum +
                " AND package_serial_fk='" + serial +"'";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * An easy way to view detailed information about a package.
     * @param accntNum: The account id associate with the package
     * @param serial: The package's serial
     * @return A ResultSet of the package.
     */
    public ResultSet viewPackageData(int accntNum, String serial){
        String query = "SELECT * from package WHERE account_number_fk=" + accntNum +
                " AND serial='" + serial +"'";
        return h2.createAndExecuteQuery(connection, query);
    }


    /**
     * The way to create an entry to rates table.
     * @param negotiatedID the negotiated id
     * @param groundRate the ground rate of the package
     * @param airRate the air rate of the package
     * @param rushRate the rush rate of the package
     * @param DRB dim_rating_break
     */
    public void CreateRate(int negotiatedID, int groundRate, int airRate, int rushRate, int DRB) {
        int employeeID = this.getId();
        String query = String.format("INSERT INTO rates(NEGOTIATED_RATE_ID, GROUND_RATE, AIR_RATE, RUSH_RATE, " +
                "DIM_RATING_BREAK, EMPLOYEEID) VALUES (\"%d\",\"%f\",\"%f\",\"%f\",\"%d\",\"%d\");",
                negotiatedID, groundRate, airRate, rushRate, DRB, employeeID);
        h2.createAndExecute(connection, query);
    }

    /**
     * The way to view the rates of the package by negotiated id
     * @param negotiatedID the negotiated id for the package
     * @return A ResultSet of the package.
     */
    public ResultSet viewRates(int negotiatedID) {
        String query = "SELECT * FROM rates WHERE negotiated_rate_id=\"" + negotiatedID + "\"";
        return h2.createAndExecuteQuery(connection, query);
    }

    public void modifyRates(int negotiatedID, int groundRate, int airRate, int rushRate, int DRB) {
        int employeeID = this.getId();
        String query = String.format("UPDATE rates SET GROUND_RATE = \"" + groundRate + "\", AIR_RATE = \"" +
                airRate + "\", RUSH_rATE = \"" + rushRate + "\", DIM_RATING_BREAK = \"" + DRB + "\", EMPLOYEEID =\""
                + employeeID + "WHERE NEGOTIATED_RATE_ID = \"" + employeeID + "\";");

        h2.createAndExecute(connection, query);
    }
}
