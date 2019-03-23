package RegExServer;

// FILE RequestFilter.java

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The RequestFilter is a filter used to parse get and post parameters
 * sent with an HttpExchange when a user connects to RegEx.
 *
 * @author Kevin Becker
 * @version 03/20/2019
 */
public class RequestFilter extends Filter {
    /**
     * Provides Filter with a description of what this filter does.
     *
     * @return A string describing this filter.
     */
    @Override
    public String description() {
        return "Parses the requested URI for request parameters";
    }

    /**
     * When doFilter is run, it parses the exchange for request data.
     *
     * @param exchange  The exchange to process parameters on.
     * @param chain  The next filter in the chain to process parameters
     *               on.
     * @throws IOException  If any IOError is encountered, this simply
     * throws it to the caller.
     */
    @Override
    public void doFilter(HttpExchange exchange, Chain chain)
            throws IOException {
        // first parses get parameters
        parseGetParameters(exchange);
        // then parses post parameters (if they exist)
        parsePostParameters(exchange);
        // then has the next filter in the chain to do its filter
        chain.doFilter(exchange);
    }

    /**
     * Parses the exchange for get parameters (parameters are found
     * in the URI with "?name1=value1" meaning the get parameter:
     * name1 = value1).
     *
     * @param exchange  The exchange to pull the get parameters
     *                  from.
     * @throws UnsupportedEncodingException If trying to decode
     * a request with an unknown encoding, it is thrown out to
     * caller.
     */
    private void parseGetParameters(HttpExchange exchange)
            throws UnsupportedEncodingException {
        // creates a new parameters map
        Map<String, Object> parameters = new HashMap<>();
        // gets the raw query to process
        String query = exchange.getRequestURI()
                               .getRawQuery();
        // parses the query and puts it in the parameters map
        parseQuery(query, parameters);
        // puts in the parameters attribute map to the attributes
        // group
        exchange.setAttribute("parameters", parameters);
    }

    /**
     * Parses the exchange for POST parameters. Post parameters
     * are sent with the request as "hidden" data (harder to
     * intercept)
     *
     * @param exchange  The exchange to pull the post parameters
     *                  from.
     * @throws IOException Throws any encountered IOException to caller.
     */
    private void parsePostParameters(HttpExchange exchange)
            throws IOException {
        // only want to process if the request has post data
        if (exchange.getRequestMethod().equalsIgnoreCase("post")) {
            // gets the parameters map from the attributes pool
            @SuppressWarnings("unchecked")
            Map<String, Object> parameters =
                (Map<String, Object>)exchange.getAttribute("parameters");

            // creates a new buffered reader to read with UTF-8
            // (forces UTF-8 instead of system default)
            BufferedReader br = new BufferedReader(
                new InputStreamReader(
                    exchange.getRequestBody(),
                    StandardCharsets.UTF_8
                )
            );
            // reads the next line in the query
            String query = br.readLine();
            // parses the query and puts it in the parameters map
            parseQuery(query, parameters);
        }
    }

    /**
     * Parses a query string and adds them to the parameters map based
     * on their keys and values.
     *
     * @param query  The query to process.
     * @param parameters  The parameters map which will house all parameters.
     * @throws UnsupportedEncodingException  Thrown if the decoder cannot
     * support the query's encoding.
     */
    @SuppressWarnings("unchecked")
    private void parseQuery(String query, Map<String, Object> parameters)
        throws UnsupportedEncodingException {
        // only continue if the query isn't null
        if (query != null) {
            // splits the query by ampersand to separate parameter pairs
            String [] pairs = query.split("[&]");
            // goes through each pair found
            for (String pair : pairs) {
                // splits up the parameter into the name and value
                String [] parameter = pair.split("[=]");

                // key and value are both first null
                String key = null;
                String value = null;
                // if the parameter actually has length
                if (parameter.length > 0) {
                    // decode the key of the parameter
                    key = URLDecoder.decode(
                        parameter[0],
                        System.getProperty("file.encoding")
                    );
                }

                // if there is a value to the parameter
                if (parameter.length > 1) {
                    // decode the value from the parameter
                    value = URLDecoder.decode(
                        parameter[1],
                        System.getProperty("file.encoding")
                    );
                }

                // now we check to see if the key already exists in the
                // map
                if (parameters.containsKey(key)) {
                    // gets the object from the map
                    Object parametersVal = parameters.get(key);

                    // determines what to do with the object
                    if(parametersVal instanceof List<?>) {
                        // cast it as a list
                        List<String> values = (List<String>)parametersVal;
                        // add the value to the list
                        values.add(value);
                    } else if(parametersVal instanceof String) {
                        // creates a new list for both values to go in
                        List<String> values = new ArrayList<>();
                        // casts and adds the parameters value to the list
                        values.add((String) parametersVal);
                        // add the new value to the list as well
                        values.add(value);
                        // puts in place the key and the new list
                        parameters.put(key, values);
                    }
                // if the key doesn't exist, just put the parameter
                } else {
                    parameters.put(key, value);
                }
            }
        }
    }
}
