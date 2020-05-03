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

}


