package be.limero.main;

import akka.actor.*;
import be.limero.actor.HttpReceiver;
import be.limero.actor.MqttBroker;
import be.limero.actor.MqttReceiver;
import be.limero.message.*;
import be.limero.util.Bus;
import be.limero.util.Cfg;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class Starter extends AbstractActor {
    // AKKA
    final static Logger log = LoggerFactory.getLogger(Starter.class);
    Bus bus = Bus.getBus();
    static ActorSystem _system;
    ActorRef _mqttBroker;
    ActorRef _mqttReceiver;
    ActorRef _httpReceiver;

    public void preStart() {
        context().setReceiveTimeout(Duration.create(5000, TimeUnit.MILLISECONDS));

        _mqttBroker = _system.actorOf(MqttBroker.props(Cfg.toConfig("host=localhost,port=1883,websocket_port=8081,allow_anonymous=true")), "mqttBroker");
        _mqttReceiver = _system.actorOf(MqttReceiver.props(Cfg.toConfig("url=\"tcp://localhost:1883\"")), "mqttReceiver");
        _httpReceiver = _system.actorOf(HttpReceiver.props(Cfg.toConfig("host=localhost,port=8082")), "httpReceiver");


        KV kvs[] = {new KV("host", "localhost"), new KV("port", 1883)};
        Object kv[] = {"host", "localhost", "port", 1883};
        _mqttBroker.tell(ConfigRequest.apply(null, kvs), self());
        _mqttBroker.tell(new StartRequest(), self());

        _mqttReceiver.tell(new StartRequest(), self());
    }


    public static void main(String[] args) {
        try {
            _system = ActorSystem.create("system");
            ActorRef me = _system.actorOf(Starter.props(), "starter");

            Header header = Header.apply("dst/esp32/system", 1);
            Reset reset = Reset.apply(header.inc());
            Reset reset2 = new Reset(header.inc());
            KV[] kvs = {new KV("key1", 1), new KV("key2", 123.5)};

            ConfigRequest config = ConfigRequest.apply(header.inc(), kvs);

            me.tell(reset, ActorRef.noSender());
            me.tell(reset2, ActorRef.noSender());
            me.tell(config, ActorRef.noSender());
        } catch (Exception ex) {
            log.error("failed ", ex);
        }

    }

    public static Props props() {
        return Props.create(Starter.class);
    }


    static JsonArray toJsonArray(Object[] array) {
        JsonArray json = new JsonArray();

        for (Object obj : array) {
            if (obj instanceof Integer) {
                json.add((Integer) obj);
            } else if (obj instanceof Double) {
                json.add((Double) obj);
            } else if (obj instanceof String) {
                json.add((String) obj);
            } else if (obj instanceof Boolean) {
                json.add((Boolean) obj);
            } else {
                json.add((String) obj.toString());
            }
        }
        return json;
    }


    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Reset.class, msg -> {
                    log.info("received message " + msg.toString());
                    JsonArray json = new JsonArray();
                    json.add(msg.header().source());
                    json.add(msg.header().id());
                    log.info(json.toString());
                })
                .match(ConfigRequest.class, msg -> {
                    log.info("received message " + msg.toString());
                    JsonArray json = toJsonArray(msg.toArray());
                    log.info(json.toString());
                })
                .match(ReceiveTimeout.class, msg -> {
                    log.info("still running... " + msg.toString());
                })
                .build();
    }
}