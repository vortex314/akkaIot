package be.limero.akka.message;

import java.util.HashMap;

public class Message extends HashMap<String, Object> {
	/**
	 * 
	 */
	public static final java.util.logging.Logger log = java.util.logging.Logger
			.getLogger(Message.class.getName());
	private static final long serialVersionUID = 1L;

	public static Message create(String key, String value) {
		Message msg = new Message();
		msg.put(key, value);
		return msg;
	}

	public boolean hasKeyValue(String key, String value) {
		log.info(" matchKeyValue('"+key+"','"+value+"' against :"+this);
		return containsKey(key) && ((String) get(key)).compareTo(value) == 0;
	}

	public String getString(String key) {
		try {
			String s=(String) get(key);
			return s;
		} catch (Exception e) {
			log.warning(" key="+key+" not found in message="+this);
			return "";
		} 
	}

	public static Message create(Object... objects) {
		Message msg = new Message();
		String key = "NOKEY";
		int counter = 0;
		for (Object object : objects) {
			if (counter % 2 == 0) {
				key = (String) object;
			} else {
				msg.put(key, object);
			}
			counter++;
		}
		return msg;

	}
	
	public static Message cmd(String cmd,Object... objects) {
		Message msg = Message.create("cmd",cmd);
		String key = "NOKEY";
		int counter = 0;
		for (Object object : objects) {
			if (counter % 2 == 0) {
				key = (String) object;
			} else {
				msg.put(key, object);
			}
			counter++;
		}
		return msg;

	}
}
