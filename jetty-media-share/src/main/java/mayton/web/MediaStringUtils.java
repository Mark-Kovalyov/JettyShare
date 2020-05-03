package mayton.web;

import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public class MediaStringUtils {

    // r.txt
    // r.
    // r
    public static Optional<String> getExtension(@NotNull String path) {
        int index = path.lastIndexOf('.');
        if (index == -1) return Optional.empty();
        return Optional.of(path.substring(index + 1).toLowerCase());
    }

    @NotNull
    public static String trimPrefix(@NotNull String prefix,@NotNull String arg) {
        int prefixLength = prefix.length();
        return arg.substring(prefixLength);
    }

    public static Optional<String> getLeaveFromPath(@NotNull String path) {
        if (path.isBlank() || !path.contains("/")) {
            return Optional.empty();
        }
        return Optional.of(path.substring(path.lastIndexOf("/") + 1));
    }

    public static Optional<String> cutLeaveFromPath(@NotNull String path) {
        if (path.isBlank() || !path.contains("/")) {
            return Optional.empty();
        }
        return Optional.of(path.substring(0, path.lastIndexOf("/")));
    }

}


