package RegExServer;

// FILE: RegExHttpHandler.java

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * RegExHttpHandler is the HttpHandler used to process requests
 * to the RegExHttpServer.  The Handler will send out the appropriate
 * pages based on the request URI.
 *
 * @author Kevin J. Becker (kjb2503)
 * @version 03/22/2019
 */
public class RegExHttpHandler implements HttpHandler {
    /**
     * The Map of sessions for different users.
     */
    private Map<String, RegExSession> sessions;
    private Map<String, String> fileTypeMIMES;

    /**
     * The document root of HTML files
     */
    public static final String DOCUMENT_ROOT = "./src/RegExServer/public_html";

    public RegExHttpHandler() {
        this.sessions = new HashMap<>();
        this.fileTypeMIMES = new HashMap<>();

        // PUTS OUR SUPPORTED MIME TYPES
        this.fileTypeMIMES.put("css", "text/css");
        this.fileTypeMIMES.put("js", "application/javascript");
        this.fileTypeMIMES.put("jpg", "image/jpeg");
        this.fileTypeMIMES.put("jpeg", "image/jpeg");
        this.fileTypeMIMES.put("gif", "image/gif");
        this.fileTypeMIMES.put("png", "image/png");
        this.fileTypeMIMES.put("ico", "image/x-icon");
        this.fileTypeMIMES.put("svg", "image/svg+xml");
    }

    /**
     * Gets a session ID from the cookies for a given request.
     *
     * @param cookies  The list of cookies to search for a session ID in.
     * @return The string session ID if it exists, else null.
     */
    private String getSessionId(List<String> cookies) {
        // if we have a sessionId set, that will appear here
        String sessionId = null;

        // goes through and each cookie if it exists
        if(cookies != null) {
            // searches for the session ID
            for(String cookiesLine : cookies) {
                // splits our cookies by the separator
                String [] allCookiesSplit = cookiesLine.split("; ");

                for(String cookie : allCookiesSplit) {
                    // splits the cookie by the first equals sign
                    String [] cookieSplit = cookie.split("[=]", 2);

                    // if our cookie ID is our session ID string
                    if(cookieSplit[0].equalsIgnoreCase("REGEX_SESSION")) {
                        RegExLogger.log("cookie found for new connection - " + cookieSplit[1], 1);
                        sessionId = cookieSplit[1];
                    }
                }
            }
        }

        // returns our sessionId
        return sessionId;
    }

    /**
     * Gets a session from the map for a given session ID, giving null
     * if it couldn't be found.
     *
     * @param sessionId  The session ID being looked for.
     * @return  The RegExSession corresponding to a session ID, else null.
     */
    private RegExSession getSessionFromId(String sessionId) {
        // our userRegExSession will initially be null
        RegExSession userRegExSession = null;

        // if we have a sessionId and it isn't -1
        if(sessionId != null && !sessionId.equals("-1")) {
            // grabs our user session
            userRegExSession = sessions.get(sessionId);
        }

        // returns our userRegExSession
        return userRegExSession;
    }

    /**
     * Handles a given HttpExchange which represents a single connection
     * to the server.
     *
     * @param exchange  The exchange that was created as part of the request.
     * @throws IOException  Any IOExceptions are thrown out to the caller.
     */
    public void handle(HttpExchange exchange) throws IOException {
        // logs that a new user has connected and where they're from
        RegExLogger.log("connection opened: " + exchange.getRemoteAddress().getHostName(), RegExLogger.NO_LEVEL);

        // attaches our default headers to the response
        attachDefaultHeaders(exchange);

        // extracts our host from the request header
        final String DOMAIN_ROOT = "http://" + exchange.getRequestHeaders().getFirst("Host");

        // gets our sessionId from the cookie header
        String sessionId = getSessionId(
            exchange.getRequestHeaders().get("Cookie")
        );

        // gets our session from our session ID
        RegExSession userRegExSession = getSessionFromId(sessionId);
        // the byte-level form of the response
        byte [] responseBody;
        // our response code (defaults to 200 OK)
        int responseCode = HttpURLConnection.HTTP_OK;

        // if we have a session and it is expired OR sessionId has a value but no
        // cookie was returned (most likely due to server restart, etc.)
        if((userRegExSession != null && !userRegExSession.isStillValidSession()) ||
           (sessionId != null && userRegExSession == null)) {
            // logs that the session is expired and that it will be deleted
            RegExLogger.warn("session is expired - responding with deletion request", 2);

            // puts in place our cookie deletion to remove it from the browser
            attachNewHeader(
                exchange,
                "Set-Cookie",
                Collections.singletonList("REGEX_SESSION=deleted; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT")
            );

            // trash the cookie from the map (remove it)
            this.sessions.remove(sessionId);

            // redirect user to home page
            responseCode = HttpURLConnection.HTTP_MOVED_TEMP;

            // attaches a location header for the browser to go to root (login)
            redirectUser(
                exchange,
                DOMAIN_ROOT + "/"
            );

            // empty response body
            responseBody = new byte[]{};
        } else {
            // gets the requested path (in lowercase form)
            String requestedPath = exchange.getRequestURI().getPath().toLowerCase();

            // logs the path user is requesting
            RegExLogger.log("user requesting page: " + requestedPath, 1);

            // if the requested path ends with a /
            if(requestedPath.endsWith("/")) {
                // add in 'index.html' to the end
                requestedPath = requestedPath + "index.html";
            }

            // grabs the parameters
            @SuppressWarnings("unchecked")
            Map<String, Object> requestParameters = (Map<String, Object>)exchange.getAttribute("parameters");

            // the next thing we need to do is figure out our path
            // we will only have certain pages we know about to build
            // simply check for the known ones here (they only go single
            // folder deep) anything else will be 404
            try {
                // if we're sending an HTML file we need to do some processing
                if(requestedPath.endsWith("html")) {
                    // the only thing sent here is html
                    attachNewHeader(
                        exchange,
                        "Content-Type",
                        Collections.singletonList("text/html; charset=UTF-8")
                    );

                    // checks for home index request OR if no session
                    switch (requestedPath) {
                        case "/index.html":
                            // check for posted login information
                            if (requestParameters.containsKey("login-submit")) {
                                // logs the user in (for now we just consider it valid and create
                                // the session)
                                // TODO: implement login information checking
                                if (requestParameters.get("username").equals("user") &&
                                    requestParameters.get("password").equals("pass")) {
                                    // creates our new session
                                    userRegExSession = new RegExSession(
                                        1,
                                        "Kevin",
                                        "Becker",
                                        "admin"
                                    );

                                    // puts our session into the map
                                    this.sessions.put("1", userRegExSession);

                                    // adds a new cookie header to tell the browser to keep track
                                    // of our session
                                    attachNewHeader(
                                        exchange,
                                        "Set-Cookie",
                                        Collections.singletonList("REGEX_SESSION=1")
                                    );
                                }
                            }

                            // if the user has a session and it is still valid
                            if (userRegExSession != null && userRegExSession.isStillValidSession()) {
                                // redirect them to the home page
                                responseCode = HttpURLConnection.HTTP_MOVED_TEMP;

                                // redirect user to the home page
                                redirectUser(exchange,DOMAIN_ROOT + "/home/");

                                // gets an empty response body
                                responseBody = new byte[]{};
                            } else {
                                // if they're not logged in continue on with loading login screen
                                responseBody = getFileContents(requestedPath);
                            }
                            break;
                        case "/home/index.html":
                            // gets our home page content for our user
                            responseBody = RegExHome.getPageContent(userRegExSession);
                            break;
                        default:
                            // attempt to bring up static version of the file
                            responseBody = getFileContents(requestedPath);
                    }
                    // else we send back the file contents of whatever was requested (if it exists)
                } else {
                    // always send the request for icon to the favicon
                    if(requestedPath.endsWith(".ico")) {
                        requestedPath = "/assets/images/favicon.ico";
                    }
                    attachNewHeader(
                        exchange,
                        "Content-Type",
                        Collections.singletonList(
                            this.fileTypeMIMES.get(
                                requestedPath.substring(requestedPath.lastIndexOf(".")+1)
                            )
                        )
                    );

                    // gets our response body from our contents
                    responseBody = getFileContents(requestedPath);
                }
                // FileNotFoundException thrown when a file cannot be located by the getFileContents method
            } catch(FileNotFoundException fnfe) {
                // set our response code to 404 NOT FOUND
                responseCode = HttpURLConnection.HTTP_NOT_FOUND;
                // get the error response page for our error
                responseBody = getErrorPage(responseCode);
            } catch(IOException e) {
                // logs that some exception was hit
                RegExLogger.error("issue with loading page - internal server error", 1);
                // our response code is set to 500 INTERNAL ERROR
                responseCode = HttpURLConnection.HTTP_INTERNAL_ERROR;
                // gets our response body page for our response code
                responseBody = getErrorPage(responseCode);
            }
        }

        // direct our exchange to send back the response headers
        exchange.sendResponseHeaders(
            responseCode,
            responseBody.length
        );

        // gets our response OutputStream
        OutputStream response = exchange.getResponseBody();
        // write byte representation of responseBody
        response.write(responseBody);
        // close the stream so it sends
        response.close();
    }

    // PAGE RETURN METHODS ==============================================

    /**
     * Gets the file contents based on the pathToFile (throwing a FileNotFoundException
     * if the file cannot be found).
     *
     * @param pathToFile  The path to the file.
     * @return  The byte-level data of the file at pathToFile.
     * @throws FileNotFoundException  If the file cannot be found a FileNotFoundException will be thrown.
     * @throws IOException  If the file runs into IO issues, an IOException will be thrown out.
     */
    private static byte[] getFileContents(String pathToFile) throws FileNotFoundException, IOException {
        // if the file isn't found we have to respond with a 404
        if(!new File(DOCUMENT_ROOT + pathToFile).exists()) {
            System.out.println("uh oh");
            throw new FileNotFoundException();
        }

        // attempts to return our login screen if an error is
        // encountered an IOException is thrown
        return Files.readAllBytes(
            Paths.get(DOCUMENT_ROOT + pathToFile)
        );
    }

    /**
     * Returns the byte-level contents of the specified error page.
     *
     * @param errorNum  The error number that was hit.
     * @return  The contents of the error page specified.
     */
    private static byte [] getErrorPage(int errorNum) throws IOException {
        // determines which page to send back
        switch (errorNum) {
            case 404:
                RegExLogger.warn("asset not found - sending 404", 1);
                return getFileContents("/404.shtml");
            case 403:
                RegExLogger.warn("no access - sending 403", 1);
                return getFileContents("/403.shtml");
            default:
                RegExLogger.warn(" server issue hit - sending 500", 1);
                // default to error 404
                return getFileContents("/500.shtml");
        }
    }

    /**
     * Attaches the default headers of a RegEx HTTP request.
     *
     * @param exchange  The exchange to attach the headers to.
     */
    private static void attachDefaultHeaders(HttpExchange exchange) {
        Headers responseHeaders = exchange.getResponseHeaders();
        responseHeaders.put(
            "Connection",
            Collections.singletonList("Keep-Alive")
        );
        responseHeaders.put(
            "Keep-Alive",
            Collections.singletonList("timeout=5")
        );
        responseHeaders.put(
            "Server",
            Collections.singletonList("RegExServe")
        );
    }

    /**
     * Attaches a new header with name name and body body to the exchange response.
     *
     * @param exchange  The exchange to attach the new header.
     * @param name  The name of the header to attach.
     * @param body  The "body" of the header to attach.
     */
    private static void attachNewHeader(HttpExchange exchange, String name, List<String> body) {
        // attaches the new response header
        exchange.getResponseHeaders().put(name, body);
    }

    private static void redirectUser(HttpExchange exchange, String pathToRedirect) {
        attachNewHeader(
            exchange,
        "Location",
            Collections.singletonList(pathToRedirect)
        );
    }
}
