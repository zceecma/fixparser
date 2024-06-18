package fix.parser;

import fix.message.FixMessage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.Assert.*;

public class FixParserTest {
    FixParser parser = new FixParser();

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testParseWithGoodChecksum() throws IOException {
        byte[] fixMessageString = "8=FIX.4.2\u00019=65\u000135=A\u000149=SERVER\u000156=CLIENT\u000134=177\u000152=20090107-18:15:16\u000198=0\u0001108=30\u000110=062\u0001".getBytes();
        FixMessage parsedMsg = parser.parse(fixMessageString);
        assertNotNull(parsedMsg);
    }
    @Test
    public void testParseWithBadChecksum() {
        byte[] fixMessageString = "8=FIX.4.2\u00019=65\u000135=A\u000149=SERVER\u000156=CLIENT\u000134=177\u000152=20090107-18:15:16\u000198=0\u0001108=30\u000110=063\u0001".getBytes();
        assertThrows(IOException.class, () -> parser.parse(fixMessageString));
    }

    @Test
    public void testParseWithBadTagLargerThanInteger() {
        byte[] fixMessageString = "2147483648=FIX.4.2\u00019=65\u000135=A\u000149=SERVER\u000156=CLIENT\u000134=177\u000152=20090107-18:15:16\u000198=0\u0001108=30\u000110=063\u0001".getBytes();
        assertThrows(IOException.class, () -> parser.parse(fixMessageString));
    }
    @Test
    public void testParseWithBadTagOverflow() {
        byte[] fixMessageString = "12147483648=FIX.4.2\u00019=65\u000135=A\u000149=SERVER\u000156=CLIENT\u000134=177\u000152=20090107-18:15:16\u000198=0\u0001108=30\u000110=063\u0001".getBytes();
        assertThrows(IOException.class, () -> parser.parse(fixMessageString));
    }
    @Test
    public void testParseWithBadTagCharacters() {
        byte[] fixMessageString = "12147483648=FIX.4.2\u00019=65\u000135=A\u000149=SERVER\u000156=CLIENT\u000134=177\u000152=20090107-18:15:16\u000198=0\u0001108=30\u000110=063\u0001".getBytes();
        assertThrows(IOException.class, () -> parser.parse(fixMessageString));
    }

    @Test
    public void testParseWithStringTag() throws IOException {
        byte[] fixMessageString = "8=FIX.4.2\u00019=65\u000135=A\u000149=SERVER\u000156=CLIENT\u000134=177\u000152=20090107-18:15:16\u000198=0\u0001108=30\u000110=062\u0001".getBytes();
        FixMessage parsedMsg = parser.parse(fixMessageString);
        assertEquals("FIX.4.2", parsedMsg.getTag(8));
    }

    @Test
    public void testParseWithCharTag() throws IOException {
        byte[] fixMessageString = "8=FIX.4.2\u00019=251\u000135=D\u000149=AFUNDMGR\u000156=ABROKER\u000134=2\u000152=20030615-01:14:49\u000111=12345\u00011=111111\u000163=0\u000164=20030621\u000121=3\u0001110=1000\u0001111=50000\u000155=IBM\u000148=459200101\u000122=1\u000154=1\u000160=20030615-01:14:49\u000138=5000\u000140=1\u000144=15.75\u000115=USD\u000159=0\u000110=10\u0001".getBytes();
        FixMessage parsedMsg = parser.parse(fixMessageString);
        assertEquals('0', parsedMsg.getTag(63));
    }

    @Test
    public void testParseWithPriceTag() throws IOException {
        byte[] fixMessageString = "8=FIX.4.2\u00019=251\u000135=D\u000149=AFUNDMGR\u000156=ABROKER\u000134=2\u000152=20030615-01:14:49\u000111=12345\u00011=111111\u000163=0\u000164=20030621\u000121=3\u0001110=1000\u0001111=50000\u000155=IBM\u000148=459200101\u000122=1\u000154=1\u000160=20030615-01:14:49\u000138=5000\u000140=1\u000144=15.75\u000115=USD\u000159=0\u000110=10\u0001".getBytes();
        FixMessage parsedMsg = parser.parse(fixMessageString);
        assertEquals(15.75f, parsedMsg.getTag(44));
    }

    @Test
    public void testParseWithUTCTimestampTag() throws IOException, ParseException {
        byte[] fixMessageString = "8=FIX.4.2\u00019=251\u000135=D\u000149=AFUNDMGR\u000156=ABROKER\u000134=2\u000152=20030615-01:14:49\u000111=12345\u00011=111111\u000163=0\u000164=20030621\u000121=3\u0001110=1000\u0001111=50000\u000155=IBM\u000148=459200101\u000122=1\u000154=1\u000160=20030615-01:14:49\u000138=5000\u000140=1\u000144=15.75\u000115=USD\u000159=0\u000110=10\u0001".getBytes();
        FixMessage parsedMsg = parser.parse(fixMessageString);
        assertEquals(new SimpleDateFormat("yyyyMMdd-hh:mm:ss").parse("20030615-01:14:49"), parsedMsg.getTag(60));
    }
}