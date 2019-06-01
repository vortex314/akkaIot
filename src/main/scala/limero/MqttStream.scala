package limero

import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.nio.file.Paths
import java.util.Date
import java.util.concurrent.CompletionStage

import akka.{Done, NotUsed, actor}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Cancellable, Props}
import akka.stream.alpakka.mqtt
import akka.stream.alpakka.mqtt.scaladsl.{MqttFlow, MqttSink, MqttSource}
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.scaladsl.Tcp.{IncomingConnection, ServerBinding}
import akka.stream._
import akka.stream.scaladsl.{Broadcast, FileIO, Flow, GraphDSL, Keep, Merge, RunnableGraph, Sink, Source, StreamConverters, Tcp}
import akka.util.{ByteString, CompactByteString}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import akka.stream.scaladsl.GraphDSL.Implicits._
import play.api.libs.json.{JsValue, Json, JsNumber, JsBoolean, JsString}

import scala.reflect._
import scala.reflect.runtime.universe._

import scala.collection.immutable
import scala.concurrent.Future
import scala.util.Random

case class Message(value: String, topic: String, timestamp: Long)

case class Coordinate(x: Double, y: Double)

case class Anchor(id: String, location: Coordinate)

case class AnchorDistance(distance: Double, fromAnchor: Anchor)

case class HeartBeat()

class LimeroSource(device: String, actor: String, msgType: String)

//class MqttSource[T](topic: String) extends Source[T, NotUsed]

//class MqttSink

//class MqttSource2[T](pattern:String) trait Source[T,NotUsed]

object MqttStream {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    //    implicit val mat = ActorMaterializer()

    val decider: Supervision.Decider = {
      case _: Exception => Supervision.Resume
      case _ => Supervision.Stop
    }

    implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system)
      .withSupervisionStrategy(decider))

    val connectionSettings = MqttConnectionSettings("tcp://limero.ddns.net:1883", "", new MemoryPersistence)
      .withAutomaticReconnect(true)

    val mqttSource: Source[MqttMessage, Future[Done]] =
      MqttSource.atMostOnce(
        connectionSettings.withClientId(clientId = "source-spec/source"),
        mqtt.MqttSubscriptions(Map("src/#" -> MqttQoS.atMostOnce, "dst/#" -> MqttQoS.AtLeastOnce)),
        bufferSize = 8
      )
    val weight = 0.9

    val jsw = JsNumber(weight)

    implicit val loggingAdapter = system.log

    def log[T](prefix: String): Flow[T, T, NotUsed] = Flow[T].map[T](jsv => {
      println(prefix + " : " + jsv);
      jsv
    })

    val exponentialFilter: Flow[Double, Double, NotUsed] =
      Flow[Double].scan(0.0)(
        (result, value) => result * (1.0 - weight) + weight * value)

    def scale(x1: Double, x2: Double, y1: Double, y2: Double) = Flow[Double].map[Double](d => {
      y1 + (d - x1) * (y2 - y1) / (x2 - x1)
    })

    def asBoolean() = Flow[JsValue].map[Boolean](js => js.asOpt[Boolean].get)

    def asDouble() = Flow[JsValue].map[Double](js => js.asOpt[Double].get)


    val toDouble: Flow[MqttMessage, Double, NotUsed] = Flow[MqttMessage].map[Double](msg => msg.payload.utf8String.toDouble)

    val toJson: Flow[MqttMessage, JsValue, NotUsed] = Flow[MqttMessage].map[JsValue](msg => Json.parse(msg.payload.utf8String))


    def toMqttMessage(topic: String): Flow[Double, MqttMessage, NotUsed] =
      Flow[Double].map[MqttMessage](d => MqttMessage(topic, ByteString(d.toString)))


    val mqttFlow: Flow[MqttMessage, MqttMessage, Future[Done]] =
      MqttFlow.atMostOnce(
        connectionSettings.withClientId("flow-spec/flow"),
        MqttSubscriptions("src/#", MqttQoS.AtLeastOnce),
        bufferSize = 8,
        MqttQoS.AtLeastOnce
      )

    val sink: Sink[MqttMessage, Future[Done]] =
      MqttSink(connectionSettings, MqttQoS.AtLeastOnce)

    var counter = 0

    def genClientId(prefix: String) = {
      counter += 1
      println(" new id " + counter)
      prefix + counter
    }


    def SrcDouble(topic: String) = {
      MqttSource.atMostOnce(
        connectionSettings.withClientId(genClientId("Src-")),
        mqtt.MqttSubscriptions(Map("src/" + topic -> MqttQoS.atMostOnce)),
        bufferSize = 8
      ).map[Double](msg => msg.payload.utf8String.toDouble)
    }

    def SrcBoolean(topic: String) = {
      MqttSource.atMostOnce(
        connectionSettings.withClientId(genClientId("Src-")),
        mqtt.MqttSubscriptions(Map("src/" + topic -> MqttQoS.atMostOnce)),
        bufferSize = 8
      ).map[Boolean](msg => msg.payload.utf8String.toBoolean)
    }

    def Dst(topic: String) = {
      Flow[Any].map[MqttMessage](d => MqttMessage("dst/" + topic, ByteString(d.toString))).to(
        MqttSink(connectionSettings.withClientId(genClientId("Dst-")), MqttQoS.AtLeastOnce))
    }

    RunnableGraph.fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        import akka.stream.scaladsl.GraphDSL.Implicits._

        SrcDouble("remote/controller/potLeft") ~> scale(0, 1023, -5, 5)  ~> Dst("drive/motor/targetSpeed")

        SrcDouble("remote/controller/potRight") ~> scale(0, 1023, -40, +40) ~> Dst("drive/steer/targetAngle")

        SrcDouble("remote/controller/potLeft").map[Boolean](m => m > 511) ~> Dst("remote/controller/ledLeft")

        SrcBoolean("remote/system/alive") ~> log[Boolean]("alive") ~> Dst("drive/system/keepGoing")
        ClosedShape
    }).run()
  }
}

/*

class Streams {
  var old: Double = 0

  def exponentialFilter(f: Double): Double = {
    val weight = 0.1
    old = (1 - weight) * f + weight * old
    old
  }

  def trilateration(anchorDistance: AnchorDistance): Coordinate = {
    Coordinate(1.3, 4.8)
  }

  GridSource[Double]("navigator/compass").via(exponentialFilter(_)).to(MqttSink[Coordinate]("navigator/direction")).mat()
  MqttSource[AnchorDistance]("navigator/location").via(trilateration(_)).to(MqttSink[Coordinate]("lawnmower/location"))
  MqttSource[HeartBeat]("brain/working").to(MqttSinks[HeartBeat]("motor/continue", "steer/continue"))
  MqttSource[Double]("+/anchor/x", "+/anchor/y", "+/anchor/distance")
}
*/

class MqttStream {

}
