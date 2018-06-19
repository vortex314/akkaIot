package be.limero.akka.message;

import java.util.logging.Logger;


public class Topic {
	static private Logger log= Logger.getLogger(Topic.class.getName());
	public String dir="";
	public String device="";
	public String service="";
	public String property="";

	public Topic(String topic) {
		String[] part = topic.split("/");
		int length=part.length;
		if ( length < 4 ) {
			log.warning(" topic has not enough fields : "+topic );
			return;
		}
		dir = part[0];
		device = part[1];
		service = part[2];
		property = part[3];
	}
}
