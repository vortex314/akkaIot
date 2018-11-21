package be.limero.main;

import akka.actor.*;
import be.limero.message.ConfigRequest;
import be.limero.message.Header;
import be.limero.message.KV;
import be.limero.message.Reset;
import be.limero.util.Bus;
import com.google.gson.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Array;
import scala.concurrent.duration.Duration;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class Starter extends AbstractActor {
    // AKKA
    final static Logger log = LoggerFactory.getLogger(Starter.class);
    Bus bus = Bus.getBus();


    public static void main(String[] args) {
        ActorSystem system;
        try {
            system = ActorSystem.create("system");
            ActorRef me = system.actorOf(Starter.props(), "Starter");
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

    public void preStart() {
        context().setReceiveTimeout(Duration.create(5000, TimeUnit.MILLISECONDS));
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