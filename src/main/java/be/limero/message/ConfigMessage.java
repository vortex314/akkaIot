package be.limero.message;

import com.fasterxml.jackson.databind.JsonNode;

public class ConfigMessage extends BaseMessage {
    public JsonNode getConfig() {
        return (JsonNode) payload;
    }
}
