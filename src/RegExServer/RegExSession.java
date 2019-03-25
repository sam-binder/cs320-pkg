package RegExServer;

// FILE: RegExSession.java

import java.util.Calendar;
import java.util.Date;

/**
 * A RegExSession is used to keep basic user information present
 * about a user between different page loads.
 *
 * @author Kevin J. Becker (kjb2503)
 * @version 03/21/2019
 */
public class RegExSession {
    /**
     *  The length that each session is valid for initially is
     *  24 hours.
     */
    private static final int SESSION_LENGTH_HOURS = 24;

    // PUBLIC SESSION VARIABLES

    /**
     * The user's ID in the database.
     */
    public int accountNumber;

    /**
     * The account type of the user (customer, accounting_employee, package_employee)
     */
    public String accountType;

    /**
     * The username of the person this session represents.
     */
    public String userName;

    /**
     * User's password
     */
    private String password;

    /**
     * The date this session expires.
     */
    private Date expiration;

    /**
     * Creates a new RegExSession based on the userId of the
     * person.
     */
    public RegExSession(String accountType, String userName, String password) {
        // sets our userId
        this.accountType = accountType;

        // sets userName and password
        this.userName = userName;
        this.password = password;

        // creates our calendar
        Calendar sessionExpire = Calendar.getInstance();
        // sets our time to the current date
        sessionExpire.setTime(new Date());
        // adds session length to that
        sessionExpire.add(Calendar.HOUR_OF_DAY, SESSION_LENGTH_HOURS);

        // sets in place our expiration date
        this.expiration = sessionExpire.getTime();
    }

    /**
     * Tests the session expiration to see if the current date is
     * before or after the session expiration.
     *
     * @return  True if the current date is before the expiration; false otherwise.
     */
    public synchronized boolean isStillValidSession() {
        // returns whether the expiration is after the current date
        // if this returns false the current date is past the expiration
        return this.expiration.after(new Date());
    }

    /**
     * Adds two hours to the expiration of the session
     */
    public synchronized void extend() {
        // creates a new calendar object based on current expiration
        Calendar newSessionExpire = Calendar.getInstance();
        newSessionExpire.setTime(this.expiration);
        // adds hours many hours to the current expiration
        newSessionExpire.add(Calendar.HOUR_OF_DAY, 2);

        // sets in place our new expiration
        this.expiration = newSessionExpire.getTime();
    }

    /**
     * Determines if the session is within an hour of expiration.
     *
     * @return  True if the session is within an hour of expiration; else false.
     */
    public synchronized boolean withinHourOfExpiration() {
        // creates a new calendar with date representation of three
        // hours from now
        Calendar oneHourFromNow = Calendar.getInstance();
        oneHourFromNow.setTime(new Date());
        oneHourFromNow.add(Calendar.HOUR_OF_DAY, 1);

        // returns true if the expiration is BEFORE three hours from now
        return this.expiration.before(oneHourFromNow.getTime());
    }

    public String replaceVarPlaceholders(String pageContent) {
        return pageContent.replace("@{user-name}", this.userName)
                          .replace("@{user-type}", this.accountType);
    }
}
