package mayton.web;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaStringUtils {

    public static Optional<Pair<Long,Long>> decodeRange(String rangeAttribute) {
        Pattern pattern = Pattern.compile("(?<from>\\d+)-(?<to>\\d+)");
        Matcher m = pattern.matcher(rangeAttribute);

        long from = Integer.parseInt(m.group("from"));
        long to = Integer.parseInt(m.group("to"));

        return Optional.empty();
    }

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
        if (StringUtil.isBlank(path) || !path.contains("/")) {
            return Optional.empty();
        }
        return Optional.of(path.substring(path.lastIndexOf("/") + 1));
    }

    public static Optional<String> cutLeaveFromPath(@NotNull String path) {
        if (StringUtil.isBlank(path) || !path.contains("/")) {
            return Optional.empty();
        }
        return Optional.of(path.substring(0, path.lastIndexOf("/")));
    }

}


