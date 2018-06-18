package be.limero.actor;


import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import be.limero.akka.message.Message;
import be.limero.akka.message.Topic;

public class DistanceListener extends AbstractActor {

    private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
    
    public DistanceListener() {
		log.info(" new DistanceListener() ");
	}
    
    void init() {
    	ActorSelection receiver = getContext().getSystem().actorSelection("/user/mqtt-receiver");
        receiver.tell(Message.cmd("subscribe","pattern","src/.*/dwm1000/distance"), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
        		.match(Message.class, msg-> msg.hasKeyValue("cmd", "init"),msg->{
        			log.info("init");
        			init();
        		})
        		.match(Message.class, msg-> msg.hasKeyValue("cmd", "mqtt/publish"),msg->{
        			Topic topic=new Topic(msg.getString("topic"));
        			log.info(" got distance from anchor "+ topic.device+"="+msg.getString("payload"));     			
        		})
                .build();
    }
    
    public static Props props() {
        return Props.create(DistanceListener.class);
    }
    
	@Override
	public void preStart() {
		log.info(this.getClass().getName() + " started.");
        init();
	}

	@Override
	public void postStop() {
		log.info(this.getClass().getName() + " stopped.");
	}

}