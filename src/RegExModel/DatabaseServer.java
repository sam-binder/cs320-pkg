package RegExModel;
import org.h2.tools.Server;

import java.sql.SQLException;

/**
 * Class to easily run a server for the database. This class will be used for class
 * demos, as it allows multiple concurrent accesses, but the submitted code will not
 * Use this class, as that database will operate as embedded.
 *
 * @author Walter Schaertl
 */
public class DatabaseServer {
    public static void main(String[] args){
        try {
            // Start the TCP Server
            String[] b = {"-tcpPort", "8095"};
            Server server = Server.createTcpServer(b).start();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }
}
