package mayton.web;

import java.util.Optional;

public class MediaStringUtils {

    // r.txt
    // r.
    // r
    public static Optional<String> getExtension(String path) {
        int index = path.lastIndexOf('.');
        if (index == -1) return Optional.empty();
        return Optional.of(path.substring(index + 1).toLowerCase());
    }

    public static String trimPrefix(String prefix, String arg) {
        int prefixLength = prefix.length();
        return arg.substring(prefixLength);
    }

    public static Optional<String> getLeaveFromPath(String path) {
        if (path.isBlank() || !path.contains("/")) {
            return Optional.empty();
        }
        return Optional.of(path.substring(path.lastIndexOf("/") + 1));
    }

    public static Optional<String> cutLeaveFromPath(String path) {
        if (path.isBlank() || !path.contains("/")) {
            return Optional.empty();
        }
        return Optional.of(path.substring(0, path.lastIndexOf("/")));
    }

}


