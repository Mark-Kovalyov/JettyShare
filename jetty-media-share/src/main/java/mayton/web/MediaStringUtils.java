package mayton.web;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static mayton.web.Config.FILE_PATH_SEPARATOR;
import static org.eclipse.jetty.util.StringUtil.isBlank;

public class MediaStringUtils {

    static Logger logger = LoggerFactory.getLogger("MediaStringUtils");

    public static Optional<HttpRequestRange> decodeRange(String rangeAttribute) {
        Pattern pattern = Pattern.compile("bytes=(?<from>\\d+)?-(?<to>\\d+)?");
        Matcher m = pattern.matcher(rangeAttribute);
        if (m.matches()) {
            try {
                Optional<Long> optFrom = m.group("from") == null ? Optional.empty() : Optional.of(Long.parseLong(m.group("from")));
                Optional<Long> optTo = m.group("to") == null ? Optional.empty() : Optional.of(Long.parseLong(m.group("to")));
                return Optional.of(new HttpRequestRange(optFrom, optTo));
            } catch (NumberFormatException ex) {
                logger.warn("[1] Unable to decode range from : '{}'", rangeAttribute);
                return Optional.empty();
            }
        } else {
            logger.warn("[2] Unable to decode range from : '{}'", rangeAttribute);
            return Optional.empty();
        }
    }

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

}


