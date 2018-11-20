package be.limero.test;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import be.limero.actor.LocationActor;
import be.limero.message.DataChange;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class LocationTest extends AbstractActor {
    ActorSystem system;
    ActorRef location;
    ActorRef me;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // Code executed before the first test method
    }

    public static Props props() {
        return Props.create(LocationTest.class);
    }

    @Before
    public void setup() {
        system = ActorSystem.create("test-system");
        location = system.actorOf(LocationActor.props(), "location-actor");
        me = system.actorOf(LocationTest.props(), "location-tester");
    }

    @After
    public void tearDown() {
        system.terminate();
    }

    @Test
    public void testTrilateration() {
        location.tell(new DataChange("src/anchor1/dwm1000/distance", "1000"), me);
        location.tell(new DataChange("src/anchor2/dwm1000/distance", "1000"), me);
        location.tell(new DataChange("src/anchor3/dwm1000/distance", "1000"), me);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(DataChange.class, dc -> dc.isTopic("src/lawnmower/location"), msg -> {

        }).build();
    }
}
