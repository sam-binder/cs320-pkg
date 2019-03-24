package RegExServer;

import java.io.IOException;

public abstract class RegExPage {
    public RegExPage(){}
    public abstract byte[] getPageContent() throws IOException;
}
