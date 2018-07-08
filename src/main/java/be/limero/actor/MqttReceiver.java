package be.limero.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.LoggingAdapter;
import be.limero.akka.message.DataChange;
import be.limero.akka.message.Message;
import be.limero.util.Bus;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Random;

public class MqttReceiver extends AbstractActor implements MqttCallback {
    // AKKA
    static ActorSystem system;
    static ActorRef distanceListener;
    private final LoggingAdapter log = akka.event.Logging.getLogger(ActorSystem.create("iot-system"), this);
    MqttClient _client;
    Bus bus = Bus.getBus();

    String _broker = "tcp://limero.ddns.net:1883";
    String _clientId = "client" + new Random().nextInt(100);
    MemoryPersistence _persistence = new MemoryPersistence();

    public MqttReceiver() {
        log.info(" new MqttReceiver() ");
    }

    public static void main(String[] args) {

        try {
            system = ActorSystem.create("iot-system");
            ActorRef me = system.actorOf(MqttReceiver.props(), "mqtt-receiver");
            System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
            distanceListener = system.actorOf(LocationActor.props(), "distance-listener");
            me.tell(Message.cmd("connect"), ActorRef.noSender());
        } catch (Exception ex) {
            System.out.println("MQTT failed {}" + ex);
        }

    }

    public static Props props() {
        return Props.create(MqttReceiver.class);
    }

    void connect() {
        try {
            _client = new MqttClient(_broker, _clientId, _persistence);
            _client.setCallback(this);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setAutomaticReconnect(true);
            connOpts.setConnectionTimeout(1000);
            System.out.println("Connecting to broker: " + _broker);
            _client.connect(connOpts);
            _client.subscribe("src/#");

        } catch (Exception ex) {
            log.warning("connect failed {} ", ex);
        }
    }

    void disconnect() {
        try {
            _client.disconnect();
        } catch (MqttException ex) {
            log.warning("MQTT failed {}", ex);
        }
    }

    @Override
    public void connectionLost(Throwable ex) {
        log.warning("connectionLost failed {}", ex);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // log.info("deliveryComplete " + token);
    }

    @Override
    public void messageArrived(String topic, MqttMessage msg) throws Exception {
        String payload = new String(msg.getPayload(), "UTF-8");
        if (!topic.startsWith("src/lawnmower"))
            bus.publish(new DataChange(topic, payload));
    }

    //
    // A K K A
    //
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Message.class, m -> m.hasKeyValue("cmd", "connect"), m -> connect())
                .match(Message.class, m -> m.hasKeyValue("cmd", "disconnect"), m -> disconnect())
                .match(DataChange.class, m -> {
                    MqttMessage msg = new MqttMessage();
                    msg.setPayload(m.getString().getBytes());
                    msg.setQos(0);
                    msg.setRetained(false);
                    _client.publish(m.topic, msg);
                    log.info("publish " + m.topic);
                }).matchAny(o -> log.info(" unknown message class :" + o.getClass().getName() + "=" + o)).build();
    }

    @Override
    public void preStart() {
        log.info(this.getClass().getName() + " started.");
        Bus.getBus().subscribe(getSelf(), "dst/.*");
        Bus.getBus().subscribe(getSelf(), "src/lawnmower/.*");
    }

    @Override
    public void postStop() {
        log.info(this.getClass().getName() + " stopped.");
    }

}