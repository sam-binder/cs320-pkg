package RegExServer;

public class RegExErrorDialog {
    public static String getErrorDialogHTML(String errorMsg) {
        // returns an error dialog if there needs to be one
        return (errorMsg != null) ?
            "<div class='error'>" + errorMsg + "</div>" :
            "";
    }
}
