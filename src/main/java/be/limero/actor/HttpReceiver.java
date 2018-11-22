package be.limero.actor;

import akka.actor.AbstractActor;
import akka.actor.Props;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.stream.ActorMaterializer;
import akka.util.ByteString;
import com.typesafe.config.Config;

public class HttpReceiver extends AbstractActor {
    Config _config;
    HttpReceiver(Config config){
        _config=config;
    }
    @Override
    public Receive createReceive() {
        return receiveBuilder().build();
    }

    public static Props props(Config config) {
        return Props.create(HttpReceiver.class,config);
    }

    public void preStart() {
        final Http http = Http.get(context().system());
        final ActorMaterializer materializer = ActorMaterializer.create(context().system());
        Http.get(context().system()).bindAndHandleSync(
                request -> {
                    if (request.getUri().path().equals("/"))
                        return HttpResponse.create().withEntity(ContentTypes.TEXT_HTML_UTF8,
                                ByteString.fromString("<html><body>Hello world!</body></html>"));
                    else if (request.getUri().path().equals("/ping"))
                        return HttpResponse.create().withEntity(ByteString.fromString("PONG!"));
                    else if (request.getUri().path().equals("/crash"))
                        throw new RuntimeException("BOOM!");
                    else {
                        request.discardEntityBytes(materializer);
                        return HttpResponse.create().withStatus(StatusCodes.NOT_FOUND).withEntity("Unknown resource!");
                    }
                }, ConnectHttp.toHost(_config.getString("host"), _config.getInt("port")), materializer);
    }
}
