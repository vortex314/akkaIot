package be.limero.actor;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.event.LoggingAdapter;
import be.limero.message.DataChange;
import be.limero.message.MessageJava;
import be.limero.message.StartRequest;
import be.limero.message.StopRequest;
import be.limero.util.Bus;
import com.typesafe.config.Config;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Random;

public class MqttReceiver extends AbstractActor implements MqttCallback {
    public class AB {
    }

    ;
    // AKKA
    static ActorSystem system;
    static ActorRef distanceListener;
    private final LoggingAdapter log = akka.event.Logging.getLogger(ActorSystem.create("iot-system"), this);
    MqttClient _client;
    Bus bus = Bus.getBus();

    Config _config;
    String _clientId = "client" + new Random().nextInt(100);
    MemoryPersistence _persistence = new MemoryPersistence();

    public MqttReceiver(Config config) {
        _config = config;
        log.info(" new MqttReceiver('{}') ", config);
    }

    public static Props props(Config cfg) {
        return Props.create(MqttReceiver.class, cfg);
    }

    void connect() {
        try {
            _client = new MqttClient(_config.getString("url"), _clientId, _persistence);
            _client.setCallback(this);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setAutomaticReconnect(true);
            connOpts.setConnectionTimeout(1000);
            System.out.println("Connecting to broker: " + _config.getString("url"));
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

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartRequest.class, m -> connect())
                .match(StopRequest.class, m -> connect())
                .match(MessageJava.class, m -> m.hasKeyValue("cmd", "disconnect"), m -> disconnect())
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