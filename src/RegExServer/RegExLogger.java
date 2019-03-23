package RegExServer;

// FILE: RegExLogger.java

import java.util.Date;

/**
 * A simple logger for the RegEx Intranet classes to use.
 *
 * @author Kevin J. Becker (kjb2503)
 * @version 03/20/2019
 */
public class RegExLogger {
    /**
     * If a logged line should have no indentation, this NO_LEVEL should be used.
     */
    public static final int NO_LEVEL = 0;

    /**
     * Set to false to stop seeing log messages.
     */
    private static final boolean DEBUG = true;

    /**
     * The ANSI text coloring "RESET" signal.
     */
    private static final String ANSI_RESET = "\u001B[0m";
    /**
     * All printed text after this string will be in red.
     */
    private static final String ANSI_RED = "\u001B[31m";
    /**
     * All printed text after this string will be in green.
     */
    private static final String ANSI_GREEN = "\u001B[32m";
    /**
     * All printed text after this string will be in yellow.
     */
    private static final String ANSI_YELLOW = "\u001B[33m";
    /**
     * All printed text after this string will be in cyan.
     */
    private static final String ANSI_CYAN = "\u001B[36m";

    /**
     * A simple logger to log information updates.
     *
     * @param msg  The message to log.
     * @param level  The level to log it at (each level is another indentation)
     */
    public static void log(String msg, int level) {
        logMsg(ANSI_GREEN, "INFO", msg, level);
    }

    /**
     * A simple logger to log warnings to the server log.
     *
     * @param msg  The warning message to log.
     * @param level  The level to log it at (each level is another indentation)
     */
    public static void warn(String msg, int level) {
        logMsg(ANSI_YELLOW, "WARNING", msg, level);
    }

    /**
     * A simple logger to log when errors are hit.
     *
     * @param msg  The error message to log.
     * @param level  The level to log it at (each level is another indentation)
     */
    public static void error(String msg, int level) {
        logMsg(ANSI_RED, "ERROR", msg, level);
    }

    /**
     * Logger used to emit messages during server bootup.
     *
     * @param msg  The message to log.
     * @param level  The level to log it at (each level is another indentation)
     */
    public static void logBootup(String msg, int level) {
        logMsg(ANSI_CYAN, "BOOT", msg, level);
    }

    /**
     * Formats the message and prints it to System.out.
     *
     * @param color  The ANSI color to print the text with.
     * @param msgType  The message type which will appear as a part of the logged message.
     * @param msg  The message to log.
     * @param level  The level to log the message at.
     */
    private static void logMsg(String color, final String msgType, String msg, int level) {
        if (DEBUG) {
            // builds our string to print
            StringBuilder logMessage = new StringBuilder();
            logMessage.append(new Date());
            logMessage.append("\t");
            logMessage.append(color);
            logMessage.append(msgType);
            logMessage.append("\t:\t");
            if(level == NO_LEVEL) {
                logMessage.append(ANSI_RESET);
            }
            // creates our message level to log with
            if(level > 0) {
                logMessage.append("|");
                for(int i = 0; i < level; ++i) {
                    logMessage.append("\t");
                }
            }
            logMessage.append(msg);
            logMessage.append(ANSI_RESET);
            // prints our message
            System.out.println(logMessage);
        }
    }
}
