package RegExServer;

// FILE: RegExHttpServer.java

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * The main server of the RegEx Intranet application.
 *
 * @author Kevin J. Becker (kjb2503)
 * @version 03/20/2019
 */
public class RegExHttpServer {
    /**
     * Runs a new instance of the RegExHttpServer.
     *
     * @param args  The command line arguments used to run the server.
     * @throws IOException  Any IOException encountered will be thrown out to jvm.
     */
    public static void main(String[] args) throws IOException {
        // log the bootup debug setting
        RegExLogger.logBootup(
            "starting server with debug logging " + (RegExLogger.DEBUG ? "ENABLED" : "DISABLED"),
            RegExLogger.NO_LEVEL
        );

        // logs that we are about to be open for business
        RegExLogger.logBootup("opening RegEx connection on port 80", RegExLogger.NO_LEVEL);

        // creates a new server
        HttpServer server = HttpServer.create(
            new InetSocketAddress(80), 0
        );
        // creates a new context from the server
        HttpContext context = server.createContext("/");
        // sets the handler (new RegExHttpHandler)
        context.setHandler(new RegExHttpHandler());
        // adds a filter
        context.getFilters().add(new RequestFilter());

        // logs that we are about to be open for business
        RegExLogger.logBootup("server is started", RegExLogger.NO_LEVEL);
        // then begins to listen for connections
        server.start();
    }
}