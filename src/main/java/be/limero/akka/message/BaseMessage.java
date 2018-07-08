package be.limero.akka.message;

import java.util.Date;
import java.util.HashMap;

public class BaseMessage {
    Date timestamp;
    HashMap<String, Object> attributes = new HashMap<String, Object>();
    Object payload;

    BaseMessage() {

    }

    public Object get(String key) {
        return attributes.get(key);
    }

    public void set(String key, Object object) {
        attributes.put(key, object);
    }

    public void set(String key, Long l) {
        set(key, Long.toString(l));
    }

    public Long getLong(String key) {
        return Long.parseLong((String) get(key));
    }

    public String toString() {
        return attributes.toString();
    }

    Object getPayload() {
        return payload;
    }

    BaseMessage createSpanChild() {
        return this;
    }

}
