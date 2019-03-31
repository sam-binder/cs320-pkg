package RegExServer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RegExTrack extends RegExPage {
    private static String pageURI = RegExHttpHandler.DOCUMENT_ROOT + "/track/index.html";

    public byte [] getPageContent() throws IOException {
        // attempts to return our login screen if an error is
        // encountered an IOException is thrown
        String pageContent = new String(
            Files.readAllBytes(
                Paths.get(pageURI)
            ),
            StandardCharsets.UTF_8
        );



        // return our page content as bytes
        return pageContent.getBytes();
    }
}
