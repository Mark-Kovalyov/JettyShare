package mayton.web;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpStringHelperTest {

    @Test
    @Disabled
    public void testRfc1123() throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        // TODO: Fix for GMP offset
        assertEquals("Wed, 21 Oct 2015 07:28:00 GMT", HttpStringHelper.formatRfc1123Date(simpleDateFormat.parse("2015-10-21 07:28:00")));
    }

}
