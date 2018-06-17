package be.limero.actor;

import java.util.HashMap;
import java.util.logging.Level;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import be.limero.akka.message.Message;

public class MqttReceiver extends AbstractActor implements MqttCallback {
	public static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(MqttReceiver.class.getName());
	MqttClient _client;
	HashMap<String, ActorRef> subscribers = new HashMap<String, ActorRef>();

	// AKKA
	static ActorSystem system;
	static ActorRef supervisor;
	static ActorRef slave;
	static ActorRef distanceListener;

	public String get_broker() {
		return _broker;
	}

	public void set_broker(String _broker) {
		this._broker = _broker;
	}

	String _broker = "tcp://limero.ddns.net:1883";
	String _clientId = "JavaSample";
	MemoryPersistence _persistence = new MemoryPersistence();

	void connect() {
		try {
			_client = new MqttClient(_broker, _clientId, _persistence);
			_client.setCallback(this);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			connOpts.setCleanSession(true);
			connOpts.setAutomaticReconnect(true);
			System.out.println("Connecting to broker: " + _broker);
			_client.connect(connOpts);
			_client.subscribe("src/#");

		} catch (Exception ex) {
			log.log(Level.WARNING, "connect failed ", ex);
		}
	}

	void disconnect() {
		try {
			_client.disconnect();
		} catch (MqttException ex) {
			log.log(Level.WARNING, "MQTT failed", ex);
		}
	}

	static void initActor() {

		supervisor = system.actorOf(IotSupervisor.props(), "iot-supervisor");
		slave = system.actorOf(IotSlave.props(), "slave");
		distanceListener = system.actorOf(DistanceListener.props(), "distance-listener");
	}

	public static void main(String[] args) {

		try {
			system = ActorSystem.create("iot-system");
			ActorRef me = system.actorOf(MqttReceiver.props());
			System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
			initActor();

			me.tell(Message.cmd("connect"), me);

		} catch (Exception ex) {
			log.log(Level.WARNING, "MQTT failed", ex);
		}

	}

	@Override
	public void connectionLost(Throwable ex) {
		log.log(Level.WARNING, "connectionLost failed", ex);
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		log.log(Level.INFO, "deliveryComplete " + token);

	}

	@Override
	public void messageArrived(String topic, MqttMessage msg) throws Exception {
		distanceListener.tell(Message.cmd("init"), getSelf());
		String payload = new String(msg.getPayload(), "UTF-8");
		log.log(Level.INFO, topic + ":" + payload);
		for (String pattern : subscribers.keySet()) {
			if (topic.matches(pattern)) {
				subscribers.get(pattern).tell(Message.create("topic", topic), getSelf());
			}
		}
	}

	//
	// A K K A
	//
	@Override
	public Receive createReceive() {
		return receiveBuilder().match(Message.class, m -> m.hasKeyValue("cmd", "subscribe"), m -> {
			subscribers.put(m.getString("pattern"), getSender());
		}).match(Message.class, m -> m.hasKeyValue("cmd", "connect"), m -> {
			connect();
		}).match(Message.class, msg -> {
			log.info(" unhandled Message : " + msg);
		}).matchAny(o -> log.info(" unknown message class :" + o.getClass().getName() + "=" + o)).build();
	}

	public static Props props() {
		return Props.create(MqttReceiver.class);
	}
	 @Override
	    public void preStart() {
	        log.info(this.getClass().getName()+" started.");
	    }

	    @Override
	    public void postStop() {
	        log.info(this.getClass().getName()+" stopped.");
	    }

}