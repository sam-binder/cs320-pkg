package RegExModel;

// FILE: Util.java

/**
 * Utility class of static methods used by the client.
 *
 * @author Sam Binder, Kevin J. Becker (kjb2503)
 * @version 04/13/2019
 */
public class Util {
    /**
     * Method used to calculate the check digit of the tracking number.
     *
     * @param trackingID The tracking number without the check digit.
     * @return The full tracking ID string with check digit.
     * @throws BadTrackingNumberFormatException If the tracking ID is not formatted correctly, this will be thrown.
     */
    public static String findCheckDigit(String trackingID) throws BadTrackingNumberFormatException {
        // checks to make sure the trackingID is the correct length
        if (trackingID.length() != 14) {
            throw new BadTrackingNumberFormatException();
        }

        // rolling-sum used to calculate the check digit
        int sum = 0;

        // goes through each character
        for (int i = 0; i < 14; i++) {
            // adds its value to the sum
            sum += trackingID.charAt(i);
        }

        // mods the sum by 17 and adds 74
        sum = (sum % 17) + 74;


        // returns the trackingID with the check character appended
        return trackingID + (char) sum;
    }

    /**
     * Validates a trackingID to ensure it is correct.
     *
     * @param trackingID The 15-character tracking ID to validate.
     * @return True if the trackingID validates, false otherwise.
     */
    public static boolean validateCheckDigit(String trackingID) {
        // returns false if the trackingID isn't the correct length
        if (trackingID.length() != 15) {
            return false;
        } else {
            // else performs the checksum
            int sum = 0;

            // goes through each character of the trackingID that isn't a check digit
            for (int i = 0; i < 14; i++) {
                sum += trackingID.charAt(i);
            }

            // normalizes the value
            sum = (sum % 17) + 74;

            // returns whether the check digit matches the sum
            return sum == trackingID.charAt(14);
        }
    }

    /**
     * A method used to generate a tracking ID String.
     *
     * @param accountID     The account ID of the package.
     * @param serviceID     The service ID of the package.
     * @param packageSerial The serial of the package.
     * @return  The tracking ID if it could be generated, else null.
     */
    public static String generateTrackingID(int accountID, int serviceID, String packageSerial){
        // the trackingID StringBuilder
        StringBuilder trackingID = new StringBuilder();

        trackingID.append(String.format("%06d", accountID));
        trackingID.append(String.format("%02d", serviceID));
        trackingID.append(packageSerial);

        // returns true if the check digit matches the sum mod 17
        try {
            return findCheckDigit(trackingID.toString());
        } catch (BadTrackingNumberFormatException btnfe) {
            return null;
        }
    }
}
