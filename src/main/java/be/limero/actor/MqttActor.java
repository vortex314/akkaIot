package be.limero.actor;

import be.limero.message.ConfigMessage;

public class MqttActor extends BaseActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ConfigMessage.class, m -> configure(m.getConfig()))
                .matchAny(m -> getContext().self().tell(m, getSender()))
                .build();
    }
}
