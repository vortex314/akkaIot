package be.limero

import java.util.Date
import java.util.concurrent.TimeUnit

import akka.NotUsed
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.alpakka.mqtt.scaladsl.MqttSource._
import akka.stream.alpakka.mqtt.{MqttConnectionSettings, MqttMessage, MqttQoS, MqttSubscriptions}
import akka.stream.alpakka.mqtt.scaladsl.{MqttSink, MqttSource}
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, RunnableGraph}
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, ClosedShape, Supervision}
import akka.util.{ByteString, Timeout}
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import GraphDSL.Implicits._
import play.api.libs.json._
import play.api.libs.json.{JsNull, JsString, JsValue, Json}

case class Message(value: JsValue, topic: String, message: String, timestamp: Long = new Date().getTime)

case class Coordinate(x: Double, y: Double)

case class Anchor(id: Int, location: Coordinate)

case class AnchorDistance(distance: Double, anchor: Anchor)

import play.api.libs.json._
import play.api.libs.functional.syntax._

object Coordinate {

  // def this(jsv: JsValue) = this((jsv \ "x").as[Double], (jsv \ "y").as[Double])

  implicit val coordinateReads: Reads[Coordinate] = (
    (JsPath \ "x").read[Double] and
      (JsPath \ "y").read[Double]
    ) (Coordinate.apply _)

  implicit val coordinateWrites: Writes[Coordinate] = (
    (JsPath \ "x").write[Double] and
      (JsPath \ "y").write[Double]
    ) (unlift(Coordinate.unapply))
}

object Anchor {
  implicit val anchorReads: Reads[Anchor] = (
    (JsPath \ "id").read[Int] and
      (JsPath).read[Coordinate]
    ) (Anchor.apply _)

  implicit val anchorWrites: Writes[Anchor] = (
    (JsPath \ "id").write[Int] and
      (JsPath).write[Coordinate]
    ) (unlift(Anchor.unapply))

}

object AnchorDistance {

  implicit val anchorDistanceReads: Reads[AnchorDistance] = (
    (JsPath \ "distance").read[Double] and
      (JsPath).read[Anchor]
    ) (AnchorDistance.apply _)

  implicit val anchorDistanceWrites: Writes[AnchorDistance] = (
    (JsPath \ "distance").write[Double] and
      (JsPath).write[Anchor]
    ) (unlift(AnchorDistance.unapply))
}

object MqttStream {
  val weight = 0.9

  def scale(x1: Double, x2: Double, y1: Double, y2: Double) = Flow[Double].map[Double](d => {
    val r = y1 + (d - x1) * (y2 - y1) / (x2 - x1)
    (math floor r * 100) / 100
  })

  def exponentialFilter(): Flow[Double, Double, NotUsed] =
    Flow[Double].scan(0.0)(
      (result, value) => result * (1.0 - weight) + weight * value)

  def main(args: Array[String]): Unit = {

    val jsv: JsValue = Json.parse("""{"x":10,"y":100,"id":1234,"distance":1.234,"location":{"x":10,"y":101}}""")

    val anchor = jsv.as[AnchorDistance]

    implicit val system = ActorSystem()

    val tril: ActorRef = system.actorOf(Props[Trilateration], "trilateration")

    val decider: Supervision.Decider = {
      case _: Exception => Supervision.Resume
      case _ => Supervision.Stop
    }

    implicit val materializer = ActorMaterializer(ActorMaterializerSettings(system)
      .withSupervisionStrategy(decider))

    val connectionSettings = MqttConnectionSettings("tcp://limero.ddns.net:1883", "", new MemoryPersistence)
      .withAutomaticReconnect(true)

    implicit val loggingAdapter = system.log

    def log[T](prefix: String): Flow[T, T, NotUsed] = Flow[T].map[T](jsv => {
      println(prefix + " : " + jsv);
      jsv
    })

    def threshold(v: Double) = Flow[Double].map[Double](d => {
      if (d.abs < v) 0
      else d
    })

    def trilateration() = Flow[AnchorDistance].map[Coordinate](ad => {
      Coordinate(0, ad.anchor.location.y)
    })

    var counter = 0

    def genClientId(prefix: String) = {
      counter += 1
      println(" new id " + counter)
      prefix + counter
    }


    def SrcDouble(topic: String) = {
      atMostOnce(
        connectionSettings.withClientId(genClientId("Src-")),
        MqttSubscriptions(Map("src/" + topic -> MqttQoS.atMostOnce)),
        bufferSize = 8
      ).map[Double](msg => msg.payload.utf8String.toDouble)
    }

    def SrcBoolean(topic: String) = {
      atMostOnce(
        connectionSettings.withClientId(genClientId("Src-")),
        MqttSubscriptions(Map("src/" + topic -> MqttQoS.atMostOnce)),
        bufferSize = 8
      ).map[Boolean](msg => msg.payload.utf8String.toBoolean)
    }

    def Src(topic: String, prefix: String = "src/") = atMostOnce(
      connectionSettings.withClientId(genClientId("Src-")),
      MqttSubscriptions(Map(prefix + topic -> MqttQoS.atMostOnce)),
      bufferSize = 8
    ).map[JsValue](msg => {
      var json = Json.obj("message" -> msg.payload.utf8String, "topic" -> msg.topic)
      val jsv = Json.parse(msg.payload.utf8String)
      json += ("value" -> jsv)
      /*      if (jsv.isInstanceOf[JsBoolean]) json += ("boolean", jsv)
            if (jsv.isInstanceOf[JsNumber]) json += ("double", jsv)
            if (jsv.isInstanceOf[JsString]) json += ("string", jsv)
            if (jsv.isInstanceOf[JsArray]) json += ("array", jsv)
            if ( jsv.isInstanceOf[JsObject]) json += ( "object" -> jsv.as[JsObject])*/
      json
    })


    def Dst(topic: String) = {
      Flow[Any].map[MqttMessage](d => MqttMessage("dst/" + topic, ByteString(d.toString))).to(
        MqttSink(connectionSettings.withClientId(genClientId("Dst-")), MqttQoS.AtLeastOnce))
    }
    import akka.pattern.ask
    implicit val askTimeout = Timeout(5, TimeUnit.SECONDS)


    RunnableGraph.fromGraph(GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._

        Src("remote/controller/potLeft").map(jsv => (jsv \ "value").as[Double]) ~> scale(0, 1023, -5, 5) ~> threshold(0.1) ~> Dst("drive/motor/targetSpeed")

        Src("remote/controller/potRight").map(jsv => (jsv \ "value").as[Double]) ~> scale(0, 1023, -90, +90) ~> Dst("drive/steer/targetAngle")

        SrcDouble("remote/controller/potRight").map(m => m > 511) ~> Dst("remote/controller/ledRight")

        Src("tag/dwm1000/anchor").map(jsv => jsv.
          as[AnchorDistance]).map(anchorDistance => (tril ? anchorDistance).
          mapTo[Option[Coordinate]]).
          filter(opt => opt.isCompleted) ~> Dst("lawnmower/location/coordinate")

        Src("remote/controller/buttonLeft").map(_.as[Boolean]) ~> Dst("pi2/wiring/gpio6")

        Src("pi2/wiring/gpio6") ~> Dst("remote/controller/ledLeft")

        Src("remote/system/alive").map(_.as[Boolean]) ~> log[Boolean]("alive") ~> Dst("drive/system/keepGoing")

        Src("+/system/+").map(jsv => (jsv \ "value").as[Double]) ~> log[Double]("addOne") ~> Dst("brain/addOne")

        Src("+/system/alive").filter(jsv => (jsv \ "topic").as[String].contains("remote"))

        //        Src("+/system/alive")~> replace[Boolean]("boolean",x => !(x))~>Dst("brain/system/dead")

        ClosedShape
    }).run()
  }
}

class MqttStream {

}
