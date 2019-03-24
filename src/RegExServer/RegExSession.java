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
     *  The length that each session is valid for intially is
     *  24 hours.
     */
    private static final int SESSION_LENGTH_HOURS = 24;

    // PUBLIC SESSION VARIABLES

    /**
     * The user's ID in the database.
     */
    public int userId;

    /**
     * The username of the person this session represents.
     */
    public String userName;

    /**
     * The first name of the person this session represents.
     */
    public String firstName;

    /**
     * The last name of the person this session represents.
     */
    public String lastName;

    /**
     * The date this session expires.
     */
    private Date expiration;

    /**
     * Creates a new RegExSession based on the userId of the
     * person.
     *
     * @param userId  The userId of the person this session represents.
     */
    public RegExSession(int userId, String firstName, String lastName) {
        // sets our userId
        this.userId = userId;

        // sets first and last names
        this.firstName = firstName;
        this.lastName = lastName;

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
     * Adds time to the expiration of a session by hours mand hours.
     *
     * @param hours  The number of hours to extend the life of this session.
     */
    public synchronized void addTimeToExpiration(int hours) {
        // creates a new calendar object based on current expiration
        Calendar newSessionExpire = Calendar.getInstance();
        newSessionExpire.setTime(this.expiration);
        // adds hours many hours to the current expiration
        newSessionExpire.add(Calendar.HOUR_OF_DAY, hours);

        // sets in place our new expiration
        this.expiration = newSessionExpire.getTime();
    }

    public String replaceVarPlaceholders(String pageContent) {
        return pageContent.replace("@{fn}", this.firstName)
                          .replace("@{ln}", this.lastName)
                          .replace("@{user_name}", this.userName)
                          .replace("@{user_id}", "" + this.userId);
    }
}
