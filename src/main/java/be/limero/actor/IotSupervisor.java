package be.limero.actor;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;

public class IotSupervisor extends AbstractActor {
    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    int slaveCounter = 0;
    ActorSelection slave;

    public static Props props() {
        return Props.create(IotSupervisor.class);
    }

    @Override
    public void preStart() {
        log.info("IoT Application started");
        slave = getContext().getSystem().actorSelection("/user/slave");
    }

    @Override
    public void postStop() {
        log.info("IoT Application stopped");
    }

    // No need to handle any messages
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, s -> s.startsWith("a"), s -> {
                    log.info(s);
                    log.info(" sender : " + getSender());
                    slave.tell(s, getSelf());
                })
                .match(String.class, s -> s.startsWith("A"), s -> {
                    log.info(s);
                    log.info(" sender : " + getSender());
                })
                .build();
    }

    ActorRef newSlave() {
        return getContext().getSystem().actorOf(Props.create(IotSlave.class, "propser"));
//        return getContext().getSystem().actorOf(IotSlave.props(),"slave"+slaveCounter++);
    }

}