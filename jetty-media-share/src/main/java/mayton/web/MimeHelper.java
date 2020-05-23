package mayton.web;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.concurrent.NotThreadSafe;
import java.io.InputStream;
import java.util.*;

import static mayton.web.MediaStringUtils.getExtension;

@NotThreadSafe
public class MimeHelper {

    static Logger logger = LoggerFactory.getLogger(MimeHelper.class);

    private Properties properties;

    // On-Demand Singleton
    private static class Singleton {
        private static final MimeHelper INSTANCE = new MimeHelper();
    }

    public static MimeHelper createInstance() {
        return Singleton.INSTANCE;
    }

    private void processExtensionNode(String prefix, LinkedHashMap<String,Object> extesionNode) {
        logger.trace("processExtensionList mime = {}", prefix);
        extesionNode.forEach((suffix, extensionArray) -> {
            if (extensionArray instanceof ArrayList) {
                ((ArrayList)extensionArray).forEach(item -> {
                    if (item instanceof String) {
                        properties.put(prefix + "/" + suffix, item);
                    }
                });
            } else {
                throw new IllegalArgumentException("Unexpected yaml entity value " + extensionArray.getClass().toString() + " (must be Array on this level!)");
            }
        });
    }

    private void processMimePrefix(String prefix, ArrayList arrayList) {
        logger.trace("processMimePrefix prefix = {}/ object type = {}", prefix, arrayList.getClass().toString());
        arrayList.forEach(value -> {
            if (value instanceof LinkedHashMap) {
                processExtensionNode(prefix, (LinkedHashMap<String, Object>) value);
            } else {
                throw new IllegalArgumentException("Unexpected yaml entity value " + value.getClass().toString() + " (must be LinkedHashList on this level!)");
            }
        });
    }

    private MimeHelper() {
        properties = new Properties();
        try {
            Yaml yaml = new Yaml();
            InputStream inputStream = MimeHelper.class.getClassLoader().getResourceAsStream("mime.yaml");
            HashMap<String, Object> yamlMap = yaml.load(inputStream);
            yamlMap.forEach((key, value) -> {
                if (value instanceof String) {
                    properties.put(key, value); // Trivial property like '
                } else if (value instanceof ArrayList) {
                    processMimePrefix(key, (ArrayList) value);
                } else {
                    throw new IllegalArgumentException("Unexpected yaml entity value " + value.getClass().toString() + " (must be String, or List only on this level!)");
                }
            });
            properties.entrySet().forEach((entry) -> {
                logger.info("property[{}] = '{}'", entry.getKey(), entry.getValue());
            });
        } catch (Exception ex) {
            logger.error(ex.toString());
        }
    }

    public String getMimeByExtenensionOrOctet(Optional<String> extension) {
        return getMimeByExtensionOrDefault(extension, "application/octet-stream");
    }

    public String getMimeByExtensionOrDefault(Optional<String> extension, @NotNull String replacement) {
        if (extension.isEmpty()) return replacement;
        return (String) properties.getOrDefault(extension.get(), replacement);
    }

    public Optional<String> getMimeByExtension(Optional<String> extension) {
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

    public boolean isVideo(@NotNull String path) {
        Optional<String> extension = getExtension(path);
        if (extension.isEmpty()) {
            return false;
        } else {
            Optional<String> mime = getMimeByExtension(extension);
            return mime.map(s -> s.startsWith("video/")).orElse(false);
        }
    }

    public boolean isPicture(@NotNull String path) {
        Optional<String> extension = getExtension(path);
        if (extension.isEmpty()) {
            return false;
        } else {
            Optional<String> mime = getMimeByExtension(extension);
            return mime.map(s -> s.startsWith("image/")).orElse(false);
        }
    }

    public boolean isAudio(@NotNull String path) {
        Optional<String> extension = getExtension(path);
        if (extension.isEmpty()) {
            return false;
        } else {
            Optional<String> mime = getMimeByExtension(extension);
            return mime.map(s -> s.startsWith("audio/")).orElse(false);
        }
    }

}
