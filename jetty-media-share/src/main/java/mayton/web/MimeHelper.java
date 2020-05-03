package mayton.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

public class MimeHelper {

    static Logger logger = LoggerFactory.getLogger("MimeHelper");

    private static Properties properties;

    static {
        properties = new Properties();
        try {
            properties.load(MimeHelper.class.getClassLoader().getResourceAsStream("mime.properties"));
            properties.stringPropertyNames().stream().forEach(item -> {
                logger.info("mime: {} -> {}", item, properties.get(item));
            });
        } catch (Exception ex) {
            logger.error(ex.toString());
        }
    }

    public static String getMimeByExtenensionOrOctet(Optional<String> extension) {
        return getMimeByExtensionOrDefault(extension, "application/octet-stream");
    }

    public static String getMimeByExtensionOrDefault(Optional<String> extension, String replacement) {
        if (extension.isEmpty()) return replacement;
        String res = (String) properties.getOrDefault(extension.get(), replacement);
        logger.info("getMimeByExtension {} -> {}", extension, res);
        return res;
    }


    public static Optional<String> getMimeByExtension(Optional<String> extension) {
        if (extension.isEmpty()) return Optional.of("application/octet-stream");
        String res = properties.getProperty(extension.get());
        if (res == null) return Optional.empty();
        logger.info("getMimeByExtension {} -> {}", extension, res);
        return Optional.of(res);
    }

    public static boolean isVideo(String path) {
        Optional<String> extension = MediaStringUtils.getExtension(path);
        if (extension.isEmpty()) {
            return false;
        } else {
            Optional<String> mime = getMimeByExtension(extension);
            if (mime.isEmpty()) return false;
            return mime.get().startsWith("video/");
        }
    }

    public static boolean isAudio(File node) throws IOException {
        String lowerFileName = node.getCanonicalFile().toString();
        Optional<String> mime = getMimeByExtension(MediaStringUtils.getExtension(lowerFileName));
        if (mime.isEmpty()) return false;
        return mime.get().startsWith("audio/");
    }

}
