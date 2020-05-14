package mayton.web;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpStringHelper {


    private static final String PATTERN_RFC1123 = "EEE, dd MMM yyyy HH:mm:ss zzz";

    private static final TimeZone GMT = TimeZone.getTimeZone("GMT");

    static Logger logger = LoggerFactory.getLogger(HttpStringHelper.class);

    private HttpStringHelper() {}

    public static Optional<HttpRequestRange> decodeRange(@NotNull String rangeAttribute) {
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


    @NotNull
    public static String formatRfc1123Date(@NotNull Date date) {
        return formatDate(date, PATTERN_RFC1123);
    }

    @NotNull
    public static String formatDate(@NotNull Date date, @NotNull String pattern) {
        SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.US);
        formatter.setTimeZone(GMT);
        return formatter.format(date);
    }


}
