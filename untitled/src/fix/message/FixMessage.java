package fix.message;

import java.util.HashMap;

public class FixMessage {
    final private HashMap<Integer, Object> fields;
    public FixMessage() {
        this.fields = new HashMap<>();
    };

    public FixMessage(HashMap<Integer, Object> fields) {
        this.fields = fields;
    }

    public Object getTag(int tag) {
        return fields.get(tag);
    }
}
