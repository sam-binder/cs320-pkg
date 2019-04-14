package RegExModel;

// FILE: BadTrackingNumberFormatException.java

/**
 * A small exception used when verifying tracking numbers.
 *
 * @author Sam Binder
 * @version 04/08/2019
 */
public class BadTrackingNumberFormatException extends Exception {
    /**
     * Just creates a blanket exception with a custom message.
     */
    public BadTrackingNumberFormatException(){
        super("That tracking number is not correct.");
    }
}
