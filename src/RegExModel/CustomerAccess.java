package RegExModel;
import java.sql.Connection;
import java.sql.ResultSet;

/**
 * Created by Walter Schaertl on 3/24/2019.
 */
public class CustomerAccess implements AutoCloseable{
    private String username;
    private Connection connection;
    private H2Access h2;

    public CustomerAccess(String username, String password){
        this.username = username;
        this.h2 = new H2Access();
        this.connection = this.h2.createConnection(username, password);
    }

    public void createAddress(){

    }

    public void sendPackage(){

    }

    public void addMoneyToAccount(){

    }

    public ResultSet viewPackageData(int accntNum, String serial){
        String query = "SELECT * from package WHERE account_number_fk=" + accntNum +
                " AND serial='" + serial +"'";
        return h2.createAndExecuteQuery(connection, query);
    }

    public ResultSet trackPackage(int accntNum, String serial){
        String query = "SELECT * from transaction WHERE account_number_fk=" + accntNum +
                " AND package_serial_fk='" + serial +"'";
        return h2.createAndExecuteQuery(connection, query);
    }

    @Override
    public void close(){
        h2.closeConnection(this.connection);
    }
}
