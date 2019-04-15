Authors: Walter Schaertl, Sam Binder, Lyan Ye
Steps to running the Database

During presentation, connections to the database will be over TCP, to allow
concurrent access and move the demo along. This allows a customer to be logged
into the web interface at the same time package movers are moving the customers
package along. However, for submission, database connections will be embedded,
and only one process can connect at a time.

The database files are included in the zip and therefor there should not be a need
for any custom set up. However, in the case of catastrophe, the main method of
H2Access has self evident controls for starting a new database from scratch. This
should not be run under normal circumstances.

To access the database as a customer, run the main method in RegExServer.RegExHttpServer.
To access the database as an employee, run the main method in RegExModel.EmployeeAccess.
Again, only one of these classes can be operational at a time. The prompts for each view
are detailed and will walk the user through all functionality. For more information, see
the demonstration on wednesday.

Sample Log ins:
    Customer:
        Username: AAAA
        Password: password
        Logs in with: The web interface
        Notes: Can send packages, update their information, and track sent packages
    Account Employee:
        Username: BBBB
        Password: password
        Logs in with: The CLI
        Notes: Can generate customer invoices, edit customer billing, negotiate with
            customers, and track packages.
    Package Employee:
        Username: CCCC
        Password: password
        Logs in with: The CLI
        Notes: When working in a truck (location that starts with V, sample: V02KZKFPGUAP),
            they can pick up packages from their origins, pick up packages from hubs, and
            drop off packages from their car to a destination. That is, they can do:
            Origin -> Vehicle, Hub -> Vehicle, and Vehicle -> Destination. When working at
            a hub (denoted by location starting with an H, sample: H0CPM9PN2P5H), they can
            receive packages from trucks, that is Vehicle -> Hub. Combined this allows
            Origin -> Vehicle -> Hub -> [Vehicle -> Hub ->] Vehicle -> Destination, with
            an number of hubs. The user can see the package at each of these stages.