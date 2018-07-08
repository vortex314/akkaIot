package be.limero.actor;


import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class IotSlave extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    public static Props props() {
        return Props.create(IotSlave.class);
    }

    @Override
    public void preStart() {
        log.info("IoT Slave started");
    }

    @Override
    public void postStop() {
        log.info("IoT Slave stopped");
    }

    // No need to handle any messages
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> {
                    log.info(s);
                    log.info(" sender : " + getSender());
                    //               Thread.sleep(100);
                    getSender().tell(s.toUpperCase(), self());
                })
                .build();
    }

}