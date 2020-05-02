package mayton.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MimeHelper {

    static Logger logger = LoggerFactory.getLogger("MimeHelper");


    public static String getMimeByExtension(String extension) {
        // TODO;
        String res = extension.endsWith(".mp3") ? "audio/mpeg" : "application/octet-stream";
        logger.info("getMimeByExtension {} -> {}", extension, res);
        return res;
    }

}
