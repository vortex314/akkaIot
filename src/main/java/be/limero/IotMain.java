package be.limero;

import java.io.IOException;

import akka.actor.AbstractActor;
import akka.actor.ActorSystem;
import akka.actor.ActorRef;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import be.limero.actor.IotSlave;
import be.limero.actor.IotSupervisor;

public class IotMain extends AbstractActor {
	public static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(IotMain.class.getName());

	public static void main(String[] args) throws IOException {

		ActorSystem system = ActorSystem.create("iot-system");

		try {
			// Create top level supervisor
			ActorRef supervisor = system.actorOf(IotSupervisor.props(), "iot-supervisor");
			ActorRef slave = system.actorOf(IotSlave.props(), "slave");

			log.info(" started ");
			for (int i = 0; i < 100; i++)
				supervisor.tell("abcdef-" + i, null);
			log.info(" stopped");

			System.out.println("Press ENTER to exit the system");
			System.in.read();
			supervisor.tell("abcdefghijklmnop", supervisor);

			System.out.println("Press ENTER to exit the system");
			System.in.read();
		} finally {
			system.terminate();
		}
	}

	@Override
	public Receive createReceive() {
		return null;
	}
}