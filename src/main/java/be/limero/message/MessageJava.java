package be.limero.message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;

public class MessageJava extends HashMap<String, Object> {
    private static final long serialVersionUID = 1L;
//	private final LoggingAdapter log =akka.event.Logging.getLogger(ActorSystem.create("iot-system"),this);
    /**
     *
     */
    static Logger log = LoggerFactory.getLogger(MessageJava.class);

    public static MessageJava create(String key, String value) {
        MessageJava msg = new MessageJava();
        msg.put(key, value);
        return msg;
    }

    public static MessageJava create(Object... objects) {
        MessageJava msg = new MessageJava();
        String key = "NOKEY";
        int counter = 0;
        for (Object object : objects) {
            if (counter % 2 == 0) {
                key = (String) object;
            } else {
                msg.put(key, object);
            }
            counter++;
        }
        return msg;

    }

    public static MessageJava cmd(String cmd, Object... objects) {
        MessageJava msg = MessageJava.create("cmd", cmd);
        String key = "NOKEY";
        int counter = 0;
        for (Object object : objects) {
            if (counter % 2 == 0) {
                key = (String) object;
            } else {
                msg.put(key, object);
            }
            counter++;
        }
        return msg;

    }

    public boolean hasKeyValue(String key, String value) {
        return containsKey(key) && ((String) get(key)).compareTo(value) == 0;
    }

    public String getString(String key) {
        try {
            return (String) get(key);
        } catch (Exception e) {
            log.warn(" key=" + key + " not found in message=" + this);
            return "";
        }
    }
}
