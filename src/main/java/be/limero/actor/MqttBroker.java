package be.limero.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import be.limero.message.StartRequest;
import be.limero.message.StopRequest;
import be.limero.util.Cfg;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import com.typesafe.config.ConfigValue;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.moquette.server.Server;
import io.moquette.server.config.ClasspathResourceLoader;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.IResourceLoader;
import io.moquette.server.config.ResourceLoaderConfig;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class MqttBroker extends AbstractActor {
    final static Logger log = LoggerFactory.getLogger(MqttBroker.class);

    String _host;
    Integer _port;
    Server _mqttBroker;
    Config _config;

    MqttBroker(Config cfg) {
        _host = cfg.getString("host");
        _port = cfg.getInt("port");
        _config = cfg;
        log.info(" new MqttBroker('{},{}') ", _host, _port);
    }

    public static Props props(Config cfg) {
        return Props.create(MqttBroker.class, cfg);
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartRequest.class, m -> start())
                .match(StopRequest.class, m -> stop())
                .build();
    }

    static class PublisherListener extends AbstractInterceptHandler {

        @Override
        public String getID() {
            return "EmbeddedLauncherPublishListener";
        }

        @Override
        public void onPublish(InterceptPublishMessage msg) {
            final String decodedPayload = new String(msg.getPayload().array(), UTF_8);
            log.info("Received on topic: " + msg.getTopicName() + " content: " + decodedPayload);
        }
    }

    public void stop() {
        _mqttBroker.stopServer();
    }
    public void start() {
        try {
            IResourceLoader classpathLoader = new ClasspathResourceLoader();
            final IConfig classPathConfig = new ResourceLoaderConfig(classpathLoader);
            String cfg=("host=localhost,port=1883,websocket_port=8081,allow_anonymous=true,ikke=dikke");
            _mqttBroker = new Server();
            _mqttBroker.startServer(Cfg.toProperties(cfg));

            //           _mqttBroker.startServer(new Properties().setProperty("port","1883"));
            List<? extends InterceptHandler> userHandlers = Collections.singletonList(new PublisherListener());
            //           _mqttBroker.startServer(classPathConfig, userHandlers);

            log.info("Broker started press [CTRL+C] to stop");
            //Bind  a shutdown hook
            Runtime.getRuntime().

                    addShutdownHook(new Thread(() ->

                    {
                        log.warn("Stopping broker");
                        _mqttBroker.stopServer();
                        log.warn("Broker stopped");
                    }));
        } catch (Exception ex) {
            log.warn("MqttBroker start failed ", ex);
        }
    }

    void publish() {
        try {
            log.info("Before self publish");
            MqttPublishMessage message = MqttMessageBuilders.publish()
                    .topicName("src/broker/alive")
                    .retained(true)
//        qos(MqttQoS.AT_MOST_ONCE);
//        qQos(MqttQoS.AT_LEAST_ONCE);
                    .qos(MqttQoS.EXACTLY_ONCE)
                    .payload(Unpooled.copiedBuffer("true".getBytes(UTF_8)))
                    .build();

            _mqttBroker.internalPublish(message, "INTRLPUB");
            log.info("After self publish");
        } catch (Exception ex) {
            log.warn("MqttBroker start failed ", ex);
        }
    }


}
