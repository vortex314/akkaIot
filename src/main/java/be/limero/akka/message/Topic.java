package be.limero.akka.message;

public class Topic {
	public String dir;
	public String device;
	public String service;
	public String property;

	public Topic(String topic) {
		String[] part = topic.split("/");
		dir = part[0];
		device = part[1];
		service = part[2];
		property = part[3];
	}
}
