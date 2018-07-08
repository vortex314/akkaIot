package be.limero.akka.message;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Topic {
    static Logger log = LoggerFactory.getLogger(Message.class);
    public String dir = "";
    public String device = "";
    public String service = "";
    public String property = "";

    public Topic(String topic) {
        String[] part = topic.split("/");
        int length = part.length;
        if (length < 4) {
            log.warn(" topic has not enough fields : " + topic);
            return;
        }
        dir = part[0];
        device = part[1];
        service = part[2];
        property = part[3];
    }
}
