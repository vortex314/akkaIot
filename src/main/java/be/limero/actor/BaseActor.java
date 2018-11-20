package be.limero.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import be.limero.message.ConfigMessage;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseActor extends AbstractActor {
    private static Logger log = LoggerFactory.getLogger(BaseActor.class);
    State _state;
    JsonNode _config = null;

    ;

    BaseActor() {
        _state = State.CREATED;
    }

    public static Props props() {
        return Props.create(BaseActor.class);
    }

    void init() { // read configuration and initialize actor for next messages
        _state = State.STARTED;
    }

    void configure(JsonNode config) {
        //TODO check with existing config if not null
        _config = config;
        _state = State.CONFIGURED;
    }

    private JsonNode getConfig() {
        return _config;
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ConfigMessage.class, m -> configure(m.getConfig()))
                .build();
    }

    @Override
    public void unhandled(Object message) {
        log.warn(" unhandled message " + message);
    }

    public enum State {
        CREATED, CONFIGURED, STARTED, STOPPED
    }


}
