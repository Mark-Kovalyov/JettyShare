package mayton.web;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Optional;

import static mayton.web.Config.FILE_PATH_SEPARATOR;
import static org.eclipse.jetty.util.StringUtil.isBlank;

public class MediaStringUtils {

    static Logger logger = LoggerFactory.getLogger(MediaStringUtils.class);

    private MediaStringUtils() {}

    public static Optional<String> getExtension(@NotNull String path) {
        int index = path.lastIndexOf('.');
        if (index == -1) return Optional.empty();
        return Optional.of(path.substring(index + 1).toLowerCase());
    }

    @NotNull
    public static String trimPrefix(@NotNull String prefix, @NotNull String arg) {
        int prefixLength = prefix.length();
        return arg.substring(prefixLength);
    }

    public static Optional<String> getLeaveFromFilePath(@NotNull String path) {
        if (isBlank(path) || !path.contains(FILE_PATH_SEPARATOR)) {
            return Optional.empty();
        }
        return Optional.of(path.substring(path.lastIndexOf(FILE_PATH_SEPARATOR) + 1));
    }

    public static Optional<String> cutLeaveFromWebPath(@NotNull String path) {
        if (isBlank(path) || !path.contains("/")) {
            return Optional.empty();
        }
        return Optional.of(path.substring(0, path.lastIndexOf('/')));
    }

    public static boolean directoryContainsVideo(File[] listFiles) {
        Optional<String> res = Arrays.stream(listFiles)
                .filter(node -> !node.isDirectory())
                .map(node -> node.toPath().toString())
                .filter(MimeHelper::isVideo)
                .findAny();

        return res.isPresent();
    }

    public static boolean directoryContainsAudio(File[] listFiles) {
        Optional<String> res = Arrays.stream(listFiles)
                .filter(node -> !node.isDirectory())
                .map(node -> node.toPath().toString())
                .filter(MimeHelper::isAudio)
                .findAny();

        return res.isPresent();
    }

    public static boolean directoryContainsPictures(File[] listFiles) {
        Optional<String> res = Arrays.stream(listFiles)
                .filter(node -> !node.isDirectory())
                .map(node -> node.toPath().toString())
                .filter(MimeHelper::isPicture)
                .findAny();

        return res.isPresent();
    }

}


