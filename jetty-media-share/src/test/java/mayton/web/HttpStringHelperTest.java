package mayton.web;

import org.junit.Ignore;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

import static org.junit.Assert.assertEquals;

public class HttpStringHelperTest {

    @Test
    @Ignore
    public void testRfc1123() throws ParseException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        // TODO: Fix for GMP offset
        assertEquals("Wed, 21 Oct 2015 07:28:00 GMT", HttpStringHelper.formatRfc1123Date(simpleDateFormat.parse("2015-10-21 07:28:00")));
    }

}
