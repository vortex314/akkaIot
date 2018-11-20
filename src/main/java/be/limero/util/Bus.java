package be.limero.util;

import akka.actor.ActorRef;
import akka.event.japi.ScanningEventBus;
import be.limero.message.DataChange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bus extends ScanningEventBus<DataChange, ActorRef, String> {
    static Logger log = LoggerFactory.getLogger(Bus.class);
    static Bus theBus = null;

    static public Bus getBus() {
        if (theBus == null) {
            theBus = new Bus();
        }
        return theBus;
    }

    @Override
    public void publish(DataChange event, ActorRef subscriber) {
        subscriber.tell(event, ActorRef.noSender());
    }

    @Override
    public int compareClassifiers(String a, String b) {
        return a.compareTo(b);
    }

    @Override
    public boolean matches(String classifier, DataChange event) {
        return event.topic.matches(classifier);
    }

    @Override
    public int compareSubscribers(ActorRef a, ActorRef b) {
        return a.compareTo(b);
    }

}
