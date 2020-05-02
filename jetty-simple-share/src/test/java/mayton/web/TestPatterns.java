package mayton.web;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.regex.Matcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class TestPatterns {

    JettyShare jettyShare;

    @Before
    public void before(){
        jettyShare = new JettyShare();
    }

    @Test
    public void testIpv4(){
        Matcher m = jettyShare.ipv4address.matcher("127.0.0.1");
        assertTrue(m.matches());
        assertEquals("127.0.0.1",m.group("host"));
        assertNull(m.group("port"));
    }

    @Test
    public void testIpv4CustomPort(){
        Matcher m = jettyShare.ipv4address.matcher("127.0.0.1:12345");
        assertTrue(m.matches());
        assertEquals("127.0.0.1",m.group("host"));
        assertEquals("12345",m.group("port"));
    }

    @Test
    public void testIpv6(){
        for(String s : new String[]{
                "[::1]",
                "[2001:db8:85a3:8d3:1319:8a2e:370:7348]",
                "[2001:db8:85a3::8a2e:370:7334]"
            }) {
            Matcher m = jettyShare.ipv6address.matcher(s);
            assertTrue(m.matches());
            assertNull(m.group("port"));
        }
    }


    @Test
    public void testIpv6Ports(){
        Matcher m = jettyShare.ipv6address.matcher("[2001:db8:85a3:8d3:1319:8a2e:370:7348]:8081");
        assertTrue(m.matches());
        assertEquals("2001:db8:85a3:8d3:1319:8a2e:370:7348",m.group("host"));
        assertEquals("8081",m.group("port"));

        m = jettyShare.ipv6address.matcher("[::1]:5555");
        assertTrue(m.matches());
        assertEquals("::1",m.group("host"));
        assertEquals("5555",m.group("port"));

        m = jettyShare.ipv6address.matcher("[2001:db8:85a3::8a2e:370:7334]:65535");
        assertTrue(m.matches());
        assertEquals("2001:db8:85a3::8a2e:370:7334",m.group("host"));
        assertEquals("65535",m.group("port"));
    }



}
