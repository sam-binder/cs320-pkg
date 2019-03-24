package RegExServer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RegExHome {
    public static byte [] getPageContent(RegExSession userRegExSession) throws IOException {
        // attempts to return our login screen if an error is
        // encountered an IOException is thrown
        String pageContent = new String(
            Files.readAllBytes(
                Paths.get(
                    RegExHttpHandler.DOCUMENT_ROOT + "/home/index.html"
                )
            ),
            StandardCharsets.UTF_8
        );

        // first we need to put in our first name
        pageContent = pageContent.replace(
            "@{fn}",
            userRegExSession.firstName
        );

        // return our page content as bytes
        return pageContent.getBytes();
    }
}
