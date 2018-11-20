package be.limero.actor;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import be.limero.message.MessageJava;

public class FrameworkTest {
    static ActorSystem system;

    public static void main(String[] args) {

        try {
            system = ActorSystem.create("iot-system");
            ActorRef me = system.actorOf(BaseActor.props(), "mqtt-receiver");
            System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %2$s %5$s%6$s%n");
            ActorRef distanceListener = system.actorOf(LocationActor.props(), "distance-listener");
            me.tell(MessageJava.cmd("connect"), ActorRef.noSender());
        } catch (Exception ex) {
            System.out.println("MQTT failed {}" + ex);
        }

    }
}
