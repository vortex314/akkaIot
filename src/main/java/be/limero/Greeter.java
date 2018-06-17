package be.limero;

import akka.actor.AbstractActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class Greeter extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public  enum Msg {
        GREET, DONE
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .matchEquals(Msg.GREET, m -> {
                    System.out.println("Hello World!");
                    sender().tell(Msg.DONE, self());
                })
                .build();
    }
}