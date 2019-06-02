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

    val weight = 0.9

    implicit val loggingAdapter = system.log

    def log[T](prefix: String): Flow[T, T, NotUsed] = Flow[T].map[T](jsv => {
      println(prefix + " : " + jsv);
      jsv
    })

    val exponentialFilter: Flow[Double, Double, NotUsed] =
      Flow[Double].scan(0.0)(
        (result, value) => result * (1.0 - weight) + weight * value)

    def scale(x1: Double, x2: Double, y1: Double, y2: Double) = Flow[Double].map[Double](d => {
      val r = y1 + (d - x1) * (y2 - y1) / (x2 - x1)
      (math floor r * 100) / 100
    })

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

        SrcDouble("remote/controller/potRight").map[Boolean](m => m > 511) ~> Dst("remote/controller/ledRight")

        SrcBoolean("remote/controller/buttonLeft") ~> Dst("remote/controller/ledRight")

        SrcBoolean("remote/controller/buttonRight") ~> Dst("remote/controller/ledLeft")

        SrcBoolean("remote/system/alive") ~> log[Boolean]("alive") ~> Dst("drive/system/keepGoing")
        ClosedShape
    }).run()
  }
}

class MqttStream {

}
