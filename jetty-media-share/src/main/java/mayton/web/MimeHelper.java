package mayton.web;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Properties;

import static mayton.web.MediaStringUtils.getExtension;

public class MimeHelper {

    static Logger logger = LoggerFactory.getLogger(MimeHelper.class);

    private static Properties properties;

    static {
        properties = new Properties();
        try {
            properties.load(MimeHelper.class.getClassLoader().getResourceAsStream("mime.properties"));
            properties.stringPropertyNames()
                    .forEach(item -> logger.info("Init mime: {} -> {}", item, properties.get(item)));

        } catch (Exception ex) {
            logger.error(ex.toString());
        }
    }

    private MimeHelper(){}

    public static String getMimeByExtenensionOrOctet(Optional<String> extension) {
        return getMimeByExtensionOrDefault(extension, "application/octet-stream");
    }

    public static String getMimeByExtensionOrDefault(Optional<String> extension, @NotNull String replacement) {
        if (extension.isEmpty()) return replacement;
        return (String) properties.getOrDefault(extension.get(), replacement);
    }


    public static Optional<String> getMimeByExtension(Optional<String> extension) {
        if (extension.isEmpty()) {
            logger.trace("getMimeByExtension {} -> application/octet-stream", extension);
            return Optional.of("application/octet-stream");
        }
        String res = properties.getProperty(extension.get());
        if (res == null) {
            logger.trace("getMimeByExtension {} -> EMPTY", extension);
            return Optional.empty();
        }
        logger.trace("getMimeByExtension {} -> {}", extension, res);
        return Optional.of(res);
    }

    public static boolean isVideo(@NotNull String path) {
        Optional<String> extension = getExtension(path);
        if (extension.isEmpty()) {
            return false;
        } else {
            Optional<String> mime = getMimeByExtension(extension);
            return mime.map(s -> s.startsWith("video/")).orElse(false);
        }
    }

    public static boolean isPicture(@NotNull String path) {
        Optional<String> extension = getExtension(path);
        if (extension.isEmpty()) {
            return false;
        } else {
            Optional<String> mime = getMimeByExtension(extension);
            return mime.map(s -> s.startsWith("image/")).orElse(false);
        }
    }

    public static boolean isAudio(@NotNull String path) {
        Optional<String> extension = getExtension(path);
        if (extension.isEmpty()) {
            return false;
        } else {
            Optional<String> mime = getMimeByExtension(extension);
            return mime.map(s -> s.startsWith("audio/")).orElse(false);
        }
    }

}
