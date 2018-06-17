package be.limero.actor;


import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import be.limero.akka.message.Message;

public class DistanceListener extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    boolean firstMessage=false;
    
    void init() {
    	ActorSelection receiver = getContext().getSystem().actorSelection("/user/mqtt-receiver");
        receiver.tell(Message.cmd("subscribe","pattern","src/.*/dwm1000/distance"), getSelf());
    }

    public static Props props() {
        return Props.create(DistanceListener.class);
    }

    @Override
    public void preStart() {
        log.info("DistanceListener started");
        init();
    }
    
    public void postStart() {
    	
    }

    @Override
    public void postStop() {
        log.info("DistanceListener stopped");
    }

    // No need to handle any messages
    @Override
    public Receive createReceive() {
        return receiveBuilder()
        		.match(Message.class, msg-> msg.hasKeyValue("cmd", "init"),msg->{
        			init();
        		})
                .match(String.class,  s -> {
                    log.info(s);
                    log.info(" sender : "+getSender());
     //               Thread.sleep(100);
                    getSender().tell(s.toUpperCase(), self());
                })
                .build();
    }

}