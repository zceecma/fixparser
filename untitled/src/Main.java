import fix.parser.FixParser;

import java.io.IOException;
import java.text.ParseException;

public class Main {
public static final int NUMBER_TEST_MESSAGES = (int) 1e6;

    public static void main(String[] args) throws IOException, ParseException {
        // Press Shift+F9 to start debugging your code. We have set one breakpoint
        // for you, but you can always add more by pressing Ctrl+F8.


        byte[][] fixMessages = getTestData();

        FixParser fixParser = new FixParser();
//        FixParser fixParser = new FixParser();
        long start = System.nanoTime();
        for (int i=0; i<NUMBER_TEST_MESSAGES; i++) {
            fixParser.parse(fixMessages[i]);
//            System.out.println(new String(fixMessages[0]));
        }
        long end = System.nanoTime();
        long timeTaken = end - start;
        System.out.println(timeTaken/NUMBER_TEST_MESSAGES + " ns/msg");
        System.out.println(NUMBER_TEST_MESSAGES/(timeTaken/1e9) + " msg/s");
    }

    private static byte[][] getTestData() {
//        String fixMessageString = "8=FIX.4.29=6535=A49=SERVER56=CLIENT34=17752=20090107-18:15:1698=0108=3010=062";
        //1237 ns/msg
        String fixMessageString = "8=FIX.4.2\u00019=251\u000135=D\u000149=AFUNDMGR\u000156=ABROKER\u000134=2\u000152=20030615-01:14:49\u000111=12345\u00011=111111\u000163=0\u000164=20030621\u000121=3\u0001110=1000\u0001111=50000\u000155=IBM\u000148=459200101\u000122=1\u000154=1\u000160=20030615-01:14:49\u000138=5000\u000140=1\u000144=15.75\u000115=USD\u000159=0\u000110=10\u0001";
        //benchmark against longer message, 2709 ns/msg, 359,041 msg/s

        byte[] fixMessage = fixMessageString.getBytes();

        byte[][] fixMessages = new byte[NUMBER_TEST_MESSAGES][];
        for (int i=0; i<NUMBER_TEST_MESSAGES; i++) {
            fixMessages[i] = fixMessage;
        }
        return fixMessages;
    }
}