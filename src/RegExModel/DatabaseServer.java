package RegExModel;
import org.h2.tools.Server;

import java.sql.SQLException;

/**
 * Class to easily run a server for the database
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
