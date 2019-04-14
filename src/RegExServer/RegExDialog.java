package RegExServer;

// FILE: RegExDialog.java

/**
 * Error dialogue generator class to generate different error dialog boxes.
 *
 * @author Kevin J. Becker (kjb2503)
 * @version 03/24/2019
 */
public class RegExDialog {
    /**
     * Generates an error HTML dialog with the given message, or, if null,
     * returns an empty string.
     *
     * @param errorMsg The error message to put in the error dialog.
     * @return An error dialog HTML block.
     */
    public static String getErrorDialogHTML(String errorMsg) {
        // returns an error dialog if there needs to be one
        return (errorMsg != null) ?
                "<div class='error'>" + errorMsg + "</div>" :
                "";
    }

    /**
     * Generates an success HTML dialog with the given message, or, if null,
     * returns an empty string.
     *
     * @param successMsg  The success message to put in the error dialog.
     * @return An error dialog HTML block.
     */
    public static String getSuccessDialogHTML(String successMsg) {
        // returns a success dialog if there needs to be one
        return (successMsg != null) ?
                "<div class='success'>" + successMsg + "</div>" :
                "";
    }
}
