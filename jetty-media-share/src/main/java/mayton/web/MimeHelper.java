package mayton.web;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Properties;

import static mayton.web.MediaStringUtils.getExtension;

public class MimeHelper {

    static Logger logger = LoggerFactory.getLogger("MimeHelper");

    private static Properties properties;

    static {
        properties = new Properties();
        try {
            properties.load(MimeHelper.class.getClassLoader().getResourceAsStream("mime.properties"));
            properties.stringPropertyNames().stream().forEach(item -> {
                logger.info("Init mime: {} -> {}", item, properties.get(item));
            });
        } catch (Exception ex) {
            logger.error(ex.toString());
        }
    }

    public static String getMimeByExtenensionOrOctet(Optional<String> extension) {
        return getMimeByExtensionOrDefault(extension, "application/octet-stream");
    }

    public static String getMimeByExtensionOrDefault(Optional<String> extension, @NotNull String replacement) {
        if (!extension.isPresent()) return replacement;
        String res = (String) properties.getOrDefault(extension.get(), replacement);
        return res;
    }


    public static Optional<String> getMimeByExtension(Optional<String> extension) {
        if (!extension.isPresent()) {
            //logger.info("getMimeByExtension {} -> application/octet-stream", extension);
            return Optional.of("application/octet-stream");
        }
        String res = properties.getProperty(extension.get());
        if (res == null) {
            //logger.info("getMimeByExtension {} -> EMPTY", extension, res);
            return Optional.empty();
        }
        //logger.info("getMimeByExtension {} -> {}", extension, res);
        return Optional.of(res);
    }

    public static boolean isVideo(@NotNull String path) {
        Optional<String> extension = getExtension(path);
        if (!extension.isPresent()) {
            return false;
        } else {
            Optional<String> mime = getMimeByExtension(extension);
            if (!mime.isPresent()) return false;
            return mime.get().startsWith("video/");
        }
    }

    public static boolean isAudio(@NotNull String path) {
        Optional<String> extension = getExtension(path);
        if (!extension.isPresent()) {
            return false;
        } else {
            Optional<String> mime = getMimeByExtension(extension);
            if (!mime.isPresent()) return false;
            return mime.get().startsWith("audio/");
        }
    }

}
