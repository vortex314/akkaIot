package be.limero.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorSelection;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import be.limero.akka.message.DataChange;
import be.limero.akka.message.Message;
import be.limero.akka.message.Topic;

public class DistanceListener extends AbstractActor {

	private final LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);
	Bus bus;
	Double avg = 0.0;

	public DistanceListener() {
		log.info(" new DistanceListener() ");
	}

	void init() {
		bus = Bus.getBus();
		bus.subscribe(getSelf(), "src/.*/dwm1000/distance");
	}

	@Override
	public Receive createReceive() {
		return receiveBuilder().match(DataChange.class, msg -> {
			Topic topic = new Topic(msg.topic);
			log.info(" got distance from anchor " + topic.device + "=" + msg.getDouble());
			avg = (avg+msg.getDouble())/2;
			bus.publish(new DataChange("src/lawnmower/distance/avg", avg.toString()));
		}).build();
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