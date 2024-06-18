package fix.parser;

import fix.message.FixMessage;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import static fix.parser.FixParser.FieldType.*;

public class FixParser {
    private static final byte FIELD_DELIMITER = '\u0001';     //no embedded <SOH> chars within field values except for datatype
    private static final byte TAG_DELIMITER = '=';     //integer tag = value
    private final SimpleDateFormat utcTimestampFormat;

    enum FieldType{
        UNKNOWN_FIELD_TYPE,
        INTEGER_FIELD_TYPE,
        CURRENCY_FIELD_TYPE,
        QTY_FIELD_TYPE,
        PRICE_FIELD_TYPE,
        CHAR_FIELD_TYPE,
        STRING_FIELD_TYPE,
        UTC_TIMESTAMP_FIELD_TYPE,
        LOCAL_MKT_DATE_FIELD_TYPE,
        CHECKSUM_FIELD_TYPE
    }
    private static final HashMap<Integer, FieldType> fieldTypes = new HashMap<Integer, FieldType>() {{
        put(1, STRING_FIELD_TYPE);
        put(8, STRING_FIELD_TYPE);
        put(9, INTEGER_FIELD_TYPE);
        put(10, CHECKSUM_FIELD_TYPE);
        put(11, STRING_FIELD_TYPE);
        put(15, CURRENCY_FIELD_TYPE);
        put(21, CHAR_FIELD_TYPE);
        put(22, STRING_FIELD_TYPE);
        put(34, INTEGER_FIELD_TYPE);
        put(35, STRING_FIELD_TYPE);
        put(38, QTY_FIELD_TYPE);
        put(40, CHAR_FIELD_TYPE);
        put(44, PRICE_FIELD_TYPE);
        put(48, STRING_FIELD_TYPE);
        put(49, STRING_FIELD_TYPE);
        put(52, UTC_TIMESTAMP_FIELD_TYPE);
        put(54, CHAR_FIELD_TYPE);
        put(55, STRING_FIELD_TYPE);
        put(56, STRING_FIELD_TYPE);
        put(59, CHAR_FIELD_TYPE);
        put(60, UTC_TIMESTAMP_FIELD_TYPE);
        put(63, CHAR_FIELD_TYPE);
        put(64, LOCAL_MKT_DATE_FIELD_TYPE);
        put(98, INTEGER_FIELD_TYPE);
        put(108, INTEGER_FIELD_TYPE);
        put(110, QTY_FIELD_TYPE);
        put(111, QTY_FIELD_TYPE);
    }};


    //reuse fields
    byte checksum;       //modulo 256 of every byte including <SOH> preceding check sum field;            //integer, unique, positive character digits with no leading zeros
    byte fieldChecksum;       //modulo 256 of every byte including <SOH> preceding check sum field;            //integer, unique, positive character digits with no leading zeros
    HashMap<Integer, Object> fields = new HashMap<>();

    public FixParser() {
        utcTimestampFormat = new SimpleDateFormat("yyyyMMdd-hh:mm:ss");
    }

    //tag should not be empty, missing delimiter, value empty, value contain <SOH>, or data not immediately preceded by its length field

    //character digits Characters 0x30 through 0x39

    //tag integer unique

    //datatype data special handling
    public FixMessage parse(byte[] rawMsg) throws IOException {
        fields.clear();
        checksum = '\u0000';
        fieldChecksum = '\u0000';
        int msgLen = rawMsg.length;
        int tagStart = 0;
        int valueStart = -1;
        int p = 0;
        int tag = 0;
        while(p < msgLen) {
            byte b = rawMsg[p];
            fieldChecksum += b;
            if (b == TAG_DELIMITER) {
                if (!isTagValueValid(tagStart, p, tag)) {
                    throw new IOException("tag is incorrect");
                }
//                System.out.println(String.format("parsed tag %d", tag));
                valueStart = p + 1;
            } else if (b == FIELD_DELIMITER) {
                if (tag == 10) {
                    byte expectedChecksum = parseChecksum(Arrays.copyOfRange(rawMsg, valueStart, p));
                    if (checksum != expectedChecksum) {
                        throw new IOException(String.format("checksum %d did not match %d", (checksum + 256) % 256, (expectedChecksum + 256) % 256));
                    }
                } else {
                    if (fieldTypes.get(tag) == null) {
                        throw new IOException();
                    }
                    Object value = parseValue(Arrays.copyOfRange(rawMsg, valueStart, p), fieldTypes.get(tag));
                    fields.put(tag, value);
                    checksum += fieldChecksum;
                    fieldChecksum = '\u0000';
                }
                valueStart = -1;
                tagStart = p + 1;
                tag = 0;
            } else if (valueStart == -1) {
                tag *= 10;
                if (b < '0' ||  b > '9') {
                    throw new IOException(String.format("tag has unexpected character at byte %d", p));
                }
                tag += b - '0';
            }
            p++;
        }
        FixMessage fixMessage = new FixMessage(fields);
        return fixMessage;
    }

    private byte parseChecksum(byte[] bytes) {
        byte value = '\u0000';
        for (byte b : bytes) {
            value *= 10;
            value += b - '0';
        }
        return value;
    }

    private Object parseValue(byte[] bytes, FieldType type) throws IOException {
        if (type == null) {
            throw new IOException("tag definition not defined");
        }
        switch (type) {
            case INTEGER_FIELD_TYPE:
                //todo validation similar to tag
                int intValue = 0;
                for (byte b : bytes) {
                    intValue *= 10;
                    intValue += b - '0';
                }
                return intValue;
            case CURRENCY_FIELD_TYPE:
                //todo validation 3 character and use predefined for currency
            case STRING_FIELD_TYPE:
                return new String(bytes);
            case QTY_FIELD_TYPE:
            case PRICE_FIELD_TYPE:
                //todo optimize
//                int characteristicValue = 0;
//                for (byte b : bytes) {
//                    characteristicValue *= 10;
//                    characteristicValue += b - '0';
//                    if (b == '.') {
//                        break;
//                    }
//                }
//                int mantissaValue = 0;
//                for (byte b : bytes) {
//                    mantissaValue *= 10;
//                    mantissaValue += b - '0';
//                }
                return Float.parseFloat(new String(bytes));
            case CHAR_FIELD_TYPE:
                return (char) (bytes[0] & 0xFF);
            case UTC_TIMESTAMP_FIELD_TYPE:
                //todo handle alternate format
                try {
                    return utcTimestampFormat.parse(new String(bytes));
                } catch (ParseException e) {
                    throw new IOException(String.format("timestamp field was incorrectly formatted %s", new String(bytes)), e);
                }
            case LOCAL_MKT_DATE_FIELD_TYPE:
                return null;
            default:
                throw new IOException("tag definition not defined");
        }
    }

    private boolean isTagValueValid(int tagStart, int tagEnd, int tag) {
        if (tag < 0) {          //overflow
            System.err.printf("tag specified in bytes %d to %d is larger than integer range%n", tagStart, tagEnd);
            return false;
        }
        if (tagEnd - tagStart >  10) {      //larger than int max
            System.err.printf("tag specified in bytes %d to %d is larger than integer range%n", tagStart, tagEnd);
            return false;
        }
        if (fieldTypes.get(tag) == null) {
            System.err.printf("tag %d not defined in system%n", tag);
            return false;
        }
        return true;
    }
}
