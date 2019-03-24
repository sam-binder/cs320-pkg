package RegExModel;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.SQLException;

/**
 * Created by Walter Schaertl on 3/23/2019.
 */
public class EmployeeAccess implements AutoCloseable{
    private String username, type;
    private Connection connection;
    private H2Access h2;

    public EmployeeAccess(String username, String password, String type){
        this.username = username;
        this.type = type;
        this.h2 = new H2Access();
        this.connection = this.h2.createConnection(username, password);
    }

    // Returns the employee id
    public int getId() {
        String query = "SELECT account_number_fk FROM user where username='" + username + "'";
        ResultSet r = h2.createAndExecuteQuery(connection, query);
        try {
            if (r.next())
                return r.getInt(1);
        }catch (SQLException e) {}
        return -1;
    }

    @Override
    public void close(){
        h2.closeConnection(this.connection);
    }

    public void updatePackageLocation(String locationId, int acctNumber, String pkgSerial){
        Date date = new Date(System.currentTimeMillis());
        Time time = new Time(System.currentTimeMillis());
        int employeeId = getId();
        String query = String.format("INSERT INTO transaction(DATE, TIME, EMPLOYEE_ID_FK, LOCATION_ID_FK, " +
                        "ACCOUNT_NUMBER_FK, PACKAGE_SERIAL_FK) VALUES (\'%s\',\'%s\',\'%s\',\'%s\',\'%s\',\'%s\');",
                        date, time, employeeId, locationId, acctNumber, pkgSerial);
        h2.createAndExecute(connection, query);
    }

    public ResultSet getCustomers(){
        String query = "SELECT * from customer";
        return h2.createAndExecuteQuery(connection, query);
    }

    public ResultSet getCustomersWhere(String conditional){
        String query = "SELECT * from customer WHERE " +  conditional;
        return h2.createAndExecuteQuery(connection, query);
}

    public ResultSet viewCustomerBilling(int acctNumber){
        String query = "SELECT * from billing WHERE account_number_fk=" + acctNumber;
        return h2.createAndExecuteQuery(connection, query);
    }

    public ResultSet viewPackageHistory(int accntNum, String serial){
        String query = "SELECT * from transaction WHERE account_number_fk=" + accntNum +
                " AND package_serial_fk='" + serial +"'";
        return h2.createAndExecuteQuery(connection, query);
    }

    public ResultSet viewPackageData(int accntNum, String serial){
        String query = "SELECT * from package WHERE account_number_fk=" + accntNum +
                " AND serial='" + serial +"'";
        return h2.createAndExecuteQuery(connection, query);
    }

}
